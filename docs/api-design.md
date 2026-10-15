# 接口设计

版本：规划 v1，2026-10-15。所有 URL 以 `/api` 为前缀。目标契约要在对应任务中逐步落地，不能作为当前服务行为说明。

## 1. 当前已有接口

| 名称 | 方法和 URL | 当前输入 | 当前返回 | 证据与不足 |
| --- | --- | --- | --- | --- |
| 商品列表 | GET `/api/goods` | 无 | 原始 `List<Goods>` JSON 数组 | 历史联调成功，无分页 |
| 商品详情 | GET `/api/goods/{id}` | Long 路径 ID | 原始 Goods；找不到时可能为空响应体 | 历史联调成功，未实现 404 业务处理 |
| 新增商品 | POST `/api/goods` | 直接绑定 Goods | boolean | 历史 true 和数据库记录已验证，未限制可写字段 |
| 修改商品 | PUT `/api/goods` | 直接绑定 Goods，含 id | boolean | 方法已写、编译通过，未确认本次修改测试 |

以上四个方法都位于 `GoodsController`。当前没有下面规划的包装响应、DTO、校验、分页、启停用、单据、流水或缓存接口。

## 2. 目标公共约定

### 2.1 响应

成功响应采用 `ApiResponse<T>`：

```json
{
  "code": "OK",
  "message": "成功",
  "data": {"id": 2}
}
```

失败响应沿用同一结构，`data` 通常为 null；字段校验失败可用 `data.errors` 返回 `{field,message}` 数组。响应不暴露数据库 SQL、堆栈或密码。

分页 `data` 结构：`records`（当前页数组）、`total`（Long 总数）、`page`、`size`。公共分页参数 `page=1`、`size=20`，page≥1，size 在 1～100。空页返回 records=[]。

成功查询/修改/确认/取消返回 HTTP 200；创建返回 HTTP 201。异常使用真实 HTTP 错误状态，不全部返回 200。

### 2.2 目标错误码

| HTTP | code | 情况 |
| --- | --- | --- |
| 400 | INVALID_ARGUMENT | 字段/数量/分页/日期不合法、重复明细、未知可写字段、空更新 |
| 404 | GOODS_NOT_FOUND | 商品不存在 |
| 404 | ORDER_NOT_FOUND | 单据不存在 |
| 409 | GOODS_CODE_DUPLICATE | 商品编码重复，包括并发插入触发唯一约束 |
| 409 | GOODS_DISABLED | 停用商品不允许出入库 |
| 409 | ORDER_STATE_CONFLICT | 取消后确认、确认后取消等非法流转 |
| 409 | INSUFFICIENT_STOCK | 出库数量超过余额 |
| 409 | STOCK_LIMIT_EXCEEDED | 入库后超过 Integer 数量上限 |
| 409 | RETRYABLE_CONFLICT | 死锁、锁等待超时等，本次事务已回滚，可重试同一单据 |
| 500 | INTERNAL_ERROR | 未预期服务异常；服务端记录完整日志 |

未定义资源路径为 404，不支持的方法为 405，不支持的媒体类型为 415，语法错误的 JSON 为 400。全局异常处理保留这些协议状态。

错误消息给出可理解的业务原因，例如“商品 G002 库存不足”，客户端逻辑依据 code。

### 2.3 DTO 边界

所有写入使用专用 DTO。拒绝 DTO 契约之外的写字段，避免客户端以为库存已被修改；具体 JSON 未知字段设置在开发任务中验证。

| DTO | 允许输入与限制 |
| --- | --- |
| GoodsCreateRequest | goodsCode 必填1～64、大写字母数字短横线；goodsName 非空白1～128；category≤64；unit≤32；price 必填0～9999999999.99且最多2位小数 |
| GoodsUpdateRequest | id 必填且>0；goodsName/category/unit/price 均可省略，传入值遵守同样范围；至少一项可修改字段；不支持用 null 清空字段 |
| GoodsStatusRequest | status 必填，0 或 1 |
| InboundOrderCreateRequest | supplierName≤128；remark≤500；items 必填1～100行 |
| OutboundOrderCreateRequest | receiverName≤128；remark≤500；items 必填1～100行 |
| 单据 ItemRequest | goodsId>0，quantity 在1～2147483647，同单 goodsId 不重复 |

商品创建不接受 id、stockQuantity、status 或时间，服务端写入库存0、状态1。修改不接受 goodsCode、stockQuantity、status、时间；状态走专用接口。单据不接受客户端生成的单号、状态、快照、业务时间或流水。

现有 PUT 是按非空字段更新的学习接口；本版暂保留路径并明确此语义，不擅自改成整对象替换。若以后统一改 PATCH，须另立兼容迁移任务。

### 2.4 输出类型

- `GoodsVO`：id、goodsCode、goodsName、category、unit、price、stockQuantity、status、createdAt、updatedAt。
- `GoodsCatalogVO`：id、goodsCode、goodsName、category、unit、price、status，不含余额及更新时间；Redis 阶段使用。
- `OrderSummaryVO`：id、orderNo、status、supplierName 或 receiverName、remark、confirmedAt、cancelledAt、createdAt、updatedAt。
- `OrderDetailVO`：OrderSummaryVO 的字段加 items；每项含 id、goodsId、goodsCodeSnapshot、goodsNameSnapshot、unitSnapshot、quantity。
- `InventoryVO`：goodsId、goodsCode、goodsName、unit、status、stockQuantity。
- `StockMovementVO`：id、goodsId、sourceType、sourceId、sourceNo、quantityChange、beforeQuantity、afterQuantity、createdAt。

时间按 Asia/Shanghai 的本地 ISO 格式，例如 `2026-10-15T10:00:00`；数量为整数，价格为十进制数，库存不能由客户端计算结果回传覆盖。

## 3. 商品模块目标接口

表中返回类型指成功响应的 `data`。所有接口均可能出现公共输入错误；列出的业务异常需要专门验收。

| 名称 | 方法 / URL | 参数 | 返回 | 处理逻辑 | 主要异常 |
| --- | --- | --- | --- | --- | --- |
| 旧列表兼容 | GET `/api/goods` | 无 | GoodsVO[] | 暂时保留学习入口，包装响应迁移时同步更新客户端；新查询使用分页接口 | 500 |
| 分页查询 | GET `/api/goods/page` | page,size,keyword≤128,status可选0/1 | PageResult<GoodsVO> | 编码精确或名称模糊匹配，状态过滤，id倒序；值参数化绑定 | 400 |
| 商品详情 | GET `/api/goods/{id}` | id>0 | GoodsVO | 读取MySQL，找不到转404 | 400、GOODS_NOT_FOUND |
| 新增商品 | POST `/api/goods` | GoodsCreateRequest | GoodsVO | 校验编码，唯一约束兜底，初始化库存0和状态1，返回生成ID | INVALID_ARGUMENT、GOODS_CODE_DUPLICATE |
| 修改资料 | PUT `/api/goods` | GoodsUpdateRequest | GoodsVO | 验证存在，显式更新允许字段，保持库存/状态/创建时间不变 | INVALID_ARGUMENT、GOODS_NOT_FOUND |
| 启停用 | PATCH `/api/goods/{id}/status` | 路径id；GoodsStatusRequest | GoodsVO | 锁商品行后更新状态，相同状态可直接成功，与确认事务协调 | INVALID_ARGUMENT、GOODS_NOT_FOUND、RETRYABLE_CONFLICT |
| 基础资料查询 | GET `/api/goods/{id}/catalog` | id>0 | GoodsCatalogVO | Redis阶段增加，缓存未命中读MySQL，不含库存 | GOODS_NOT_FOUND；Redis失败回源 |

当前旧列表仍是原始数组，新增/修改仍为 boolean；目标 GoodsVO/包装响应要在 T03～T05 中迁移并重新测试。完整模块验收包含这些迁移。

商品暂不提供物理删除接口。名称模糊查询不承诺普通 B-Tree 索引能优化前置百分号匹配，后续使用 EXPLAIN 评估。

## 4. 入库模块目标接口

| 名称 | 方法 / URL | 参数 | 返回 | 处理逻辑 | 主要异常 |
| --- | --- | --- | --- | --- | --- |
| 创建草稿 | POST `/api/inbound-orders` | InboundOrderCreateRequest | OrderDetailVO，201 | 校验1～100行及商品启用，生成IN单号，事务写主明细，生成资料快照，不加库存 | INVALID_ARGUMENT、GOODS_NOT_FOUND、GOODS_DISABLED |
| 草稿/历史详情 | GET `/api/inbound-orders/{id}` | id>0 | OrderDetailVO | 查询主表和全部明细快照 | ORDER_NOT_FOUND |
| 单据分页 | GET `/api/inbound-orders` | page,size,orderNo精确,status可选0/1/2,from,to | PageResult<OrderSummaryVO> | 时间半开区间、创建时间及id倒序，不返回整批明细 | INVALID_ARGUMENT |
| 确认入库 | POST `/api/inbound-orders/{id}/confirm` | id>0，无Body | OrderDetailVO | 锁单据和商品；校验状态/商品/上限；库存+流水+确认状态同事务提交 | ORDER_NOT_FOUND、ORDER_STATE_CONFLICT、GOODS_DISABLED、STOCK_LIMIT_EXCEEDED、RETRYABLE_CONFLICT |
| 取消草稿 | POST `/api/inbound-orders/{id}/cancel` | id>0，无Body | OrderDetailVO | 锁主行；草稿转取消；已取消返回原结果，不改库存 | ORDER_NOT_FOUND、ORDER_STATE_CONFLICT、RETRYABLE_CONFLICT |

重复确认已确认单返回200和原确认结果，不重复加库存。取消后的单据不能确认。创建和确认是两个独立动作，不能把创建成功当成已经入库。

请求示例（设计，不自动发送）：

```json
{
  "supplierName": "测试供应方",
  "remark": "到货入库",
  "items": [
    {"goodsId": 2, "quantity": 5}
  ]
}
```

## 5. 出库模块目标接口

| 名称 | 方法 / URL | 参数 | 返回 | 处理逻辑 | 主要异常 |
| --- | --- | --- | --- | --- | --- |
| 创建草稿 | POST `/api/outbound-orders` | OutboundOrderCreateRequest | OrderDetailVO，201 | 校验明细、商品及快照，事务保存主明细，不扣库存、不预占库存 | INVALID_ARGUMENT、GOODS_NOT_FOUND、GOODS_DISABLED |
| 单据详情 | GET `/api/outbound-orders/{id}` | id>0 | OrderDetailVO | 返回主表和明细快照 | ORDER_NOT_FOUND |
| 单据分页 | GET `/api/outbound-orders` | page,size,orderNo,status,from,to | PageResult<OrderSummaryVO> | 与入库列表相同分页规则 | INVALID_ARGUMENT |
| 确认出库 | POST `/api/outbound-orders/{id}/confirm` | id>0，无Body | OrderDetailVO | 锁单据、按商品ID升序锁余额；检查可用量；扣减+流水+状态同事务提交 | ORDER_NOT_FOUND、ORDER_STATE_CONFLICT、GOODS_DISABLED、INSUFFICIENT_STOCK、RETRYABLE_CONFLICT |
| 取消草稿 | POST `/api/outbound-orders/{id}/cancel` | id>0，无Body | OrderDetailVO | 草稿转取消，重复取消返回原结果；不恢复库存，因为草稿从未扣库存 | ORDER_NOT_FOUND、ORDER_STATE_CONFLICT、RETRYABLE_CONFLICT |

库存以确认时为准，创建草稿时有货不保证之后一定能确认。多行中任一商品不足，所有扣减、流水和状态变更均回滚。重复确认已确认单不会再次扣减。

出库创建请求与入库相同，将 supplierName 替换为 receiverName。items 只包含 goodsId 和 quantity。

## 6. 库存与流水模块目标接口

| 名称 | 方法 / URL | 参数 | 返回 | 处理逻辑 | 主要异常 |
| --- | --- | --- | --- | --- | --- |
| 库存分页 | GET `/api/inventory` | page,size,keyword≤128,status可选0/1 | PageResult<InventoryVO> | 读取goods余额，id倒序，不走Redis | INVALID_ARGUMENT |
| 商品余额 | GET `/api/inventory/{goodsId}` | goodsId>0 | InventoryVO | 实时读取MySQL，区分商品不存在和库存为0 | GOODS_NOT_FOUND |
| 库存流水分页 | GET `/api/stock-movements` | page,size,goodsId可选,sourceType可选0/1/2,sourceNo可选,from,to | PageResult<StockMovementVO> | 按商品、来源、时间筛选，createdAt及id倒序 | INVALID_ARGUMENT；给定不存在goodsId时GOODS_NOT_FOUND |

流水的 sourceId 结合 sourceType 跳转到对应单据；OPENING 来源不对应业务单据。没有直接设置库存、创建任意流水、修改流水或删除历史的 HTTP 接口。

时间筛选规则：from、to 必须同时提供，from < to；单次范围最多31天；均不提供时默认最近31天。单据和流水列表均采用该规则。

## 7. 当前修改接口的下一次验收

本轮规划不代替开发者执行写请求。下一任务只验证当前 PUT：

1. 保存一个实际存在商品的修改前详情，不假定数据库仍有ID=2。
2. 重新编译并启动最新服务，发送含该id、goodsName、price的PUT。
3. 记录响应，再GET同一个id，对比名称和价格。
4. 核对goodsCode、stockQuantity、createdAt没有意外变化。
5. 保存脱敏的请求、响应、状态码和测试时间，再标记该任务通过。

此时的 boolean 和原始详情是现状契约；在统一响应任务完成前，不用目标结构误判当前接口。

## 8. 最终业务验收用例（全部待执行）

| 场景 | 预期 |
| --- | --- |
| 新建商品 | 201，库存0，生成ID；重复编码409 |
| 读取不存在商品 | 404 GOODS_NOT_FOUND |
| 资料更新带stockQuantity | 400，余额不变化 |
| 创建入库草稿 | 草稿存在，余额及流水数量不变 |
| 确认入库5件 | 余额+5，一条流水，状态确认 |
| 重复确认同一入库单 | 200原结果，余额及流水数量不再增加 |
| 出库数量超过库存 | 409，库存、流水和草稿状态均不变 |
| 多行出库第二行失败 | 第一行也回滚，无新增流水 |
| 并发确认同一单据 | 只产生一次业务效果 |
| 不同出库单争抢同一库存 | 总扣减不超过余额，未成功单保持草稿 |
| 确认与取消并发 | 只有一个合法终态，无取消单产生库存变动 |
| Redis不可用 | 资料查询回源，库存业务仍由MySQL正常处理 |

完整自动化测试设计见 [development-plan.md](development-plan.md)，实际通过情况仅记录在 [progress.md](progress.md)。
