# 数据库设计

版本：规划 v1，2026-10-15。数据库名：`warehouse_management`。

## 1. 证据与实施边界

目前代码只映射 `goods`，仓库没有可复建的建表 SQL。本章的 `goods` 现状来自 2026-09-29 实际执行 `SHOW CREATE TABLE goods` 的记录；本次未连接数据库，不能确认之后是否有人改表。

其余五张表和所有补充约束均为设计，尚未建表。进入数据库开发任务时先重新执行只读结构核对，再由开发者写 SQL、接受 Review 后实施。这里不自动执行任何 DDL、删除或数据修复。

## 2. 表清单

| 表 | 业务作用 | 状态 |
| --- | --- | --- |
| `goods` | 商品资料及单仓库当前库存余额 | 已有实体和历史建表证据 |
| `inbound_order` | 入库单主表、状态和确认时间 | 待建 |
| `inbound_order_item` | 入库商品及数量、资料快照 | 待建 |
| `outbound_order` | 出库单主表、状态和确认时间 | 待建 |
| `outbound_order_item` | 出库商品及数量、资料快照 | 待建 |
| `stock_movement` | 期初、入库、出库的已提交库存变化 | 待建 |

单仓库继续用 `goods.stock_quantity`，暂不建立第二份可写余额。多仓库时才迁移到 `(warehouse_id, goods_id)` 唯一的库存表。

## 3. 通用约定

- 存储引擎 InnoDB，字符集 utf8mb4；沿用现有 `utf8mb4_unicode_ci`，商品编码服务端规范为大写。
- 主键和关联 ID 为 `BIGINT UNSIGNED`，匹配现有 `goods.id`，Java 为 `Long`。业务只使用 `1～Long.MAX_VALUE`，不使用无符号类型的高半区。
- 新表主键 `AUTO_INCREMENT`，实体明确使用 `IdType.AUTO`。API 创建请求不能传主键。
- 数量字段与 Java Integer 对齐：余额 `0～2147483647`，明细数量 `1～2147483647`。MySQL 无符号类型本身不足以保证 Java 上限。
- 时间为 `DATETIME`，数据库和应用约定 Asia/Shanghai；接口为无时区 ISO 时间字符串，查询采用 `[from, to)`，不把它误标成 UTC。
- `created_at` 使用 `DEFAULT CURRENT_TIMESTAMP`，可变主表的 `updated_at` 使用自动更新时间；确认/取消时间由服务端在状态流转时写入。
- 外键列与被引用列类型、无符号属性相同。外键采用 `ON DELETE RESTRICT / ON UPDATE RESTRICT`，不级联清除业务历史。
- 数据库约束和应用校验共同防错；DDL 在实际 MySQL 8 环境中验证。非空字段不代表非空字符串，字符串仍需应用校验。

## 4. goods：现有表与目标规则

| 字段 | 历史 DDL 类型/约束 | 业务含义与目标补充 |
| --- | --- | --- |
| `id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT`，PK | 商品 ID；已有 Java 自增注解 |
| `goods_code` | `VARCHAR(64) NOT NULL`，唯一索引 | 编码；目标为大写字母/数字/短横线，创建后不可改 |
| `goods_name` | `VARCHAR(128) NOT NULL` | 名称；目标非空白 |
| `category` | `VARCHAR(64) NULL` | 分类 |
| `unit` | `VARCHAR(32) NULL` | 计量单位，历史快照会保存该值 |
| `price` | `DECIMAL(12,2) NOT NULL DEFAULT 0.00` | 参考价，Java BigDecimal；已有非负 CHECK |
| `stock_quantity` | `INT UNSIGNED NOT NULL DEFAULT 0` | 当前余额；目标补 `<=2147483647` 约束及应用校验 |
| `status` | `TINYINT NOT NULL DEFAULT 1` | `1=启用，0=停用`；已有 CHECK |
| `created_at` | `DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP` | 创建时间，只读 |
| `updated_at` | `DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | 最近更新时间，只读 |

历史已有：`PRIMARY KEY(id)`、`uk_goods_code(goods_code)`、价格非负及状态合法 CHECK。

目标新增：`idx_goods_status_id(status,id)`，用于状态过滤；补数量上限 CHECK。实施前核对现有数据是否符合约束，不能直接假定符合。

新商品库存固定为 0；现有商品不被自动清零。资料修改禁止写 `stock_quantity`、`created_at`、`updated_at`，状态通过单独接口修改。

本版保留商品和历史记录，使用停用替代商品物理删除。

## 5. inbound_order：入库主表（待建）

| 字段 | 类型与约束 | 作用 |
| --- | --- | --- |
| `id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT`，PK | 单据主键 |
| `order_no` | `VARCHAR(40) NOT NULL`，UNIQUE | 服务端生成单号：`IN` 加 32 位 UUID 十六进制字符 |
| `supplier_name` | `VARCHAR(128) NULL` | 供应方名称，非供应商外键 |
| `status` | `TINYINT NOT NULL DEFAULT 0` | 0 草稿、1 已确认、2 已取消 |
| `remark` | `VARCHAR(500) NULL` | 单据备注 |
| `confirmed_at` | `DATETIME NULL` | 确认时间 |
| `cancelled_at` | `DATETIME NULL` | 取消时间 |
| `created_at` | `DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP` | 创建时间 |
| `updated_at` | `DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | 状态更新时间 |

约束：状态只能为 0/1/2；草稿两个业务时间均为空，确认只有 `confirmed_at` 非空，取消只有 `cancelled_at` 非空。

索引：`uk_inbound_order_no(order_no)`、`idx_inbound_status_created(status,created_at,id)`、`idx_inbound_created(created_at,id)`。前者查单号，后二者用于带/不带状态的时间分页；以后通过 EXPLAIN 和实际查询保留必要索引。

不冗余保存总数量和金额。总数量在查询时从明细计算并使用 Long 接收，避免多明细累加溢出。

## 6. inbound_order_item：入库明细（待建）

| 字段 | 类型与约束 | 作用 |
| --- | --- | --- |
| `id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT`，PK | 明细主键 |
| `order_id` | `BIGINT UNSIGNED NOT NULL`，FK → inbound_order.id | 所属入库单 |
| `goods_id` | `BIGINT UNSIGNED NOT NULL`，FK → goods.id | 商品 |
| `goods_code_snapshot` | `VARCHAR(64) NOT NULL` | 创建草稿时从商品表读取的编码 |
| `goods_name_snapshot` | `VARCHAR(128) NOT NULL` | 商品名称快照 |
| `unit_snapshot` | `VARCHAR(32) NULL` | 单位快照 |
| `quantity` | `INT UNSIGNED NOT NULL`，CHECK 1～2147483647 | 本次入库数量 |
| `created_at` | `DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP` | 创建时间 |

索引：`uk_inbound_item_goods(order_id,goods_id)` 防止同单重复商品，并覆盖按单查询；`idx_inbound_item_goods(goods_id)` 支持商品关联与外键。快照必须由服务端填写，不接受客户端伪造。

明细创建后不可修改，本版以取消草稿并重建解决填写错误。

## 7. outbound_order：出库主表（待建）

| 字段 | 类型与约束 | 作用 |
| --- | --- | --- |
| `id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT`，PK | 单据主键 |
| `order_no` | `VARCHAR(40) NOT NULL`，UNIQUE | `OUT` 加 32 位 UUID 十六进制字符 |
| `receiver_name` | `VARCHAR(128) NULL` | 收货方名称 |
| `status` | `TINYINT NOT NULL DEFAULT 0` | 0 草稿、1 已确认、2 已取消 |
| `remark` | `VARCHAR(500) NULL` | 备注 |
| `confirmed_at` | `DATETIME NULL` | 确认时间 |
| `cancelled_at` | `DATETIME NULL` | 取消时间 |
| `created_at` | `DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP` | 创建时间 |
| `updated_at` | `DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | 更新时间 |

约束与入库主表一致。索引：`uk_outbound_order_no(order_no)`、`idx_outbound_status_created(status,created_at,id)`、`idx_outbound_created(created_at,id)`。

## 8. outbound_order_item：出库明细（待建）

| 字段 | 类型与约束 | 作用 |
| --- | --- | --- |
| `id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT`，PK | 明细主键 |
| `order_id` | `BIGINT UNSIGNED NOT NULL`，FK → outbound_order.id | 所属出库单 |
| `goods_id` | `BIGINT UNSIGNED NOT NULL`，FK → goods.id | 商品 |
| `goods_code_snapshot` | `VARCHAR(64) NOT NULL` | 商品编码快照 |
| `goods_name_snapshot` | `VARCHAR(128) NOT NULL` | 商品名称快照 |
| `unit_snapshot` | `VARCHAR(32) NULL` | 单位快照 |
| `quantity` | `INT UNSIGNED NOT NULL`，CHECK 1～2147483647 | 出库数量 |
| `created_at` | `DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP` | 创建时间 |

索引：`uk_outbound_item_goods(order_id,goods_id)`、`idx_outbound_item_goods(goods_id)`。明细同样不可修改。

## 9. stock_movement：库存流水（待建）

| 字段 | 类型与约束 | 作用 |
| --- | --- | --- |
| `id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT`，PK | 流水主键，分页排序辅助 |
| `goods_id` | `BIGINT UNSIGNED NOT NULL`，FK → goods.id | 商品 |
| `source_type` | `TINYINT NOT NULL`，CHECK 0/1/2 | 0 期初、1 入库、2 出库 |
| `source_id` | `BIGINT UNSIGNED NOT NULL` | 入/出库主键；期初时为 goods.id |
| `source_no` | `VARCHAR(40) NOT NULL` | 单号；期初为 `OPENING-{goodsId}` |
| `quantity_change` | `INT NOT NULL`，CHECK 非零且绝对值 ≤2147483647 | 入库/期初为正，出库为负 |
| `before_quantity` | `INT UNSIGNED NOT NULL`，CHECK ≤2147483647 | 变化前余额 |
| `after_quantity` | `INT UNSIGNED NOT NULL`，CHECK ≤2147483647 | 变化后余额 |
| `created_at` | `DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP` | 业务流水生成时间 |

额外规则：`after_quantity = before_quantity + quantity_change`，计算和校验使用足够宽的有符号类型；期初必须从 0 开始，出库 delta 为负，入库 delta 为正。

索引：

- `uk_movement_source_goods(source_type,source_id,goods_id)`：同来源同商品最多一条，作为重复确认的最后一道约束。
- `idx_movement_goods_created(goods_id,created_at,id)`：商品维度时间查询。
- `idx_movement_created(created_at,id)`：全仓时间查询。

`source_id` 随 `source_type` 指向不同主表，无法用一个普通外键表达；由单据 Service 在同一事务中保证来源存在。期初来源指向同一商品。不能误把此列外键只关联入库单。

本版每单同商品只出现一次，因此上述唯一键成立。将来支持分批确认时必须重新设计流水唯一键，不能直接复用。

## 10. 关联关系

```text
inbound_order  1 ── N inbound_order_item N ── 1 goods
outbound_order 1 ── N outbound_order_item N ── 1 goods
goods         1 ── N stock_movement
入/出库主表    1 ── N stock_movement（按 source_type + source_id 逻辑关联）
```

主单据和明细有物理外键；流水到商品有物理外键，流水到多种来源采用受业务约束的逻辑关联。

## 11. 数据流转和一致性

示例：某商品接入时已有库存 20。

| 操作 | 单据状态 | 商品余额 | 新流水 |
| --- | --- | --- | --- |
| 建立期初记录 | 无业务单据 | 仍为 20 | OPENING：0 → 20，delta=+20 |
| 创建入库 5 件 | DRAFT | 20 | 无 |
| 确认入库 | CONFIRMED | 25 | INBOUND：20 → 25，delta=+5 |
| 重复确认同一入库单 | 保持 CONFIRMED | 25 | 无 |
| 确认出库 8 件 | CONFIRMED | 17 | OUTBOUND：25 → 17，delta=-8 |
| 出库 18 件 | 保持 DRAFT，失败 | 17 | 无 |
| 取消草稿 | CANCELLED | 17 | 无 |

确认事务锁定单据和商品，排序加锁并检查条件更新结果；余额、流水和状态同事务提交。流水保存成功而状态失败时也必须回滚。

本版闭环形成后，对每个商品应成立：

```text
当前余额 = 期初数量 + 已提交入库数量 - 已提交出库数量
         = 该商品全部流水 quantity_change 之和
```

现有余额没有对应历史流水，在期初接入完成前不能用该公式宣称已经对账成功。

## 12. 现有数据接入与复建

1. 保存并核对实际 `SHOW CREATE TABLE goods`，由开发者建立版本化 `src/main/resources/db/schema.sql`。
2. 不删现有商品，不重排主键，不把开发库当一次性测试库。
3. 新表和约束先在独立测试库验证，再评审开发库变更。
4. 停止相关写入并开启受控事务，为每个已有正库存商品建立一条期初流水；余额为 0 的商品不生成零变动流水。
5. 期初只记账不更新余额；唯一来源键防止重复执行，发现已有业务流水或不完整期初应中止核查。
6. 期初记录和新出入库衔接后，执行余额与流水合计核对；测试使用独立数据，不能每次启动都自动初始化期初。

DDL 需要 COMMENT、明确约束和索引，但本轮只设计，不提供自动执行脚本或引入迁移框架。
