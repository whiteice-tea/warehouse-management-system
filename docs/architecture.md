# 技术架构设计

版本：规划 v1，2026-10-15。当前实现仅包含商品基础链路；以下新增包和模块均为目标设计。

## 1. 技术选型

| 技术 | 当前/目标 | 选择原因与边界 |
| --- | --- | --- |
| Java | 当前目标及本机验证均为 JDK 21 | 沿用已有环境，数量使用整数，金额使用 BigDecimal |
| Spring Boot | 当前 `4.0.8`，保持 | 已经编译并有历史启动证据，本轮不升级版本 |
| Spring Web MVC | 已有 `spring-boot-starter-webmvc` | 同步 REST 接口足够，不引入响应式栈 |
| MyBatis-Plus | 已有 Boot 4 Starter `3.5.17` | 基础 CRUD 复用；库存锁、条件更新和联表查询由开发者学习编写 SQL |
| MySQL | 目标 MySQL 8；历史本机为 8.4.11 | InnoDB 事务、行锁、唯一约束保障核心业务，本次未重新查询服务器版本 |
| Redis | 计划，尚无依赖或配置 | 用于不含库存的商品资料缓存，不保存库存事实 |
| Maven Wrapper | 已有 | 固定构建入口，减少本机 Maven 差异 |
| Lombok | 已有 | 减少实体访问器代码；关键业务仍由开发者编写 |
| 参数校验 | 计划使用 Boot 管理的 Validation 依赖 | DTO 中声明基本边界，复杂规则放 Service；开发对应任务时再引入 |
| 测试 | 已有 JUnit/Spring 测试依赖 | 后续使用 MockMvc、Mockito 和独立 MySQL 测试库，暂不引入 Docker/Testcontainers |
| Redis 客户端 | 计划 Spring Data Redis | 使用 Boot 管理的版本，不额外引入 Redisson |
| 分页 | 计划 MyBatis-Plus 分页插件 | 对应解析器是可选模块，实施时核对 3.5.17 依赖；不盲目叠加旧 Starter |

Boot 4 应使用对应的 MyBatis-Plus Boot 4 Starter，避免重复声明普通 MyBatis Starter；版本和可选模块以 [MyBatis-Plus 官方安装文档](https://baomidou.com/getting-started/install/)及当前 `pom.xml` 为准。

## 2. 整体架构

一个 Maven 工程、一个 Spring Boot 进程、一个 MySQL 数据库。Redis 在后期作为可选缓存接入。

```text
Postman / 后续前端
        ↓ HTTP + JSON
Controller（路由、绑定、基本校验、响应）
        ↓ DTO
Service（业务规则、状态流转、事务）
        ├─ Mapper → MySQL（单据、库存、流水的事实来源）
        └─ GoodsCatalogCache → Redis（后期，只缓存基础资料）
```

库存事务不调用外部接口，不等待 Redis，也不通过跨线程任务更新库存。

## 3. 实际目录

```text
src/main/java/org/whiteicetea/warehousemanagementsystem/
├── WarehouseManagementSystemApplication.java
├── controller/GoodsController.java
├── entity/Goods.java
├── mapper/GoodsMapper.java
├── service/GoodsService.java
└── service/impl/GoodsServiceImpl.java
src/main/resources/application.yaml     # 本地存在，Git 已忽略
src/test/java/org/whiteicetea/warehousemanagementsystem/
└── WarehouseManagementSystemApplicationTests.java
```

`GoodsService` 继承 `IService<Goods>`，实现类继承 `ServiceImpl<GoodsMapper, Goods>`。当前 3.5.17 的导入路径是 `com.baomidou.mybatisplus.spring.service...`，不要照搬旧课程的 `extension.service...`。

## 4. 目标目录：按任务逐步新增

```text
org.whiteicetea.warehousemanagementsystem
├── controller/       # Goods、InboundOrder、OutboundOrder、Inventory、StockMovement
├── service/
│   └── impl/         # 各模块业务实现，沿用现有结构
├── mapper/           # 表映射和必要的手写 SQL
├── entity/           # Goods + 五个新表实体
├── dto/
│   ├── goods/        # GoodsCreateRequest、GoodsUpdateRequest、GoodsStatusRequest、GoodsQuery
│   ├── inbound/      # InboundOrderCreateRequest、InboundItemRequest
│   ├── outbound/     # OutboundOrderCreateRequest、OutboundItemRequest
│   └── inventory/    # InventoryQuery、StockMovementQuery
├── vo/               # GoodsVO、GoodsCatalogVO、OrderDetailVO、InventoryVO 等
├── common/           # ApiResponse、PageResult、ErrorCode
├── exception/        # BusinessException、GlobalExceptionHandler
├── config/           # MybatisPlusConfig；Redis 阶段再加 RedisConfig
└── cache/            # Redis 阶段的 GoodsCatalogCache
```

资源后续增加 `mapper/*.xml`、`db/schema.sql`、无密码示例配置；测试资源增加独立测试库配置和可重复测试数据。此处只是目录设计，没有创建这些业务文件。

不为每一层再建抽象基类、仓储接口、领域事件总线，也不拆 Maven 子模块。未来复杂度确实增长时再调整。

## 5. 分层职责

| 层 | 负责 | 示例 |
| --- | --- | --- |
| Controller | HTTP 方法、路径参数、请求体绑定、基本校验、响应转换 | 调用 `confirm(id)` 并返回单据结果 |
| Service | 商品存在/状态检查、重复判断、状态流转、事务编排 | 确认出库并协调库存和流水 |
| Mapper | 精确 SQL、行锁、条件更新、查询结果映射 | 按商品 ID 加锁，库存足够时扣减 |
| Entity | 数据库字段映射 | `Goods.stockQuantity` 对应 `stock_quantity` |
| DTO | 客户端允许输入的字段 | 商品修改 DTO 不含库存、创建时间和状态 |
| VO | 稳定的接口输出 | 明细 VO，分页 VO，缓存资料 VO |
| 全局异常处理 | 将校验、找不到资源、唯一冲突、业务异常映射到契约 | `404 GOODS_NOT_FOUND` |

简单 DTO/VO 先手工转换，不引入 MapStruct。避免直接把 Entity 当作公共写入协议。

## 6. 模块调用关系

- 商品：`GoodsController → GoodsService → GoodsMapper`。
- 入库：`InboundOrderController → InboundOrderService → 入库主/明细 Mapper + InventoryService + StockMovementService`。
- 出库：`OutboundOrderController → OutboundOrderService → 出库主/明细 Mapper + InventoryService + StockMovementService`。
- 库存查询：`InventoryController → InventoryService → GoodsMapper`。
- 流水查询：`StockMovementController → StockMovementService → StockMovementMapper`。
- 商品基础资料缓存：`GoodsController → GoodsService → GoodsCatalogCache → Redis / GoodsMapper`。

`InventoryService` 只处理受控的库存读取、锁定和数量增减；`StockMovementService` 处理流水记录及查询。两者不反向调用单据 Service，避免循环依赖；也不开放任意库存修改 HTTP 接口。

事务由入库/出库的公开 `confirm` 方法发起。内部服务使用同一个数据源、同一个事务连接，不使用 `REQUIRES_NEW` 拆开流水提交。

## 7. 事务与并发设计

这是后续要实现和验证的设计，当前代码尚无该流程。

确认单据的伪流程：

```text
开启事务
按主键锁定单据行
若已确认，返回已有结果
若已取消，拒绝
读取不可变的草稿明细
按 goodsId 升序锁定涉及的商品行
校验商品启用、数量合法、库存够用或不溢出
执行数量更新，检查每次影响行数
写入含前后余额的库存流水
将单据从草稿更新为已确认，填写确认时间
提交事务
任何一步异常 → 整体回滚
```

同单据确认与取消都先锁单据主行，避免同时成功；不同单据按相同商品 ID 顺序加锁，降低死锁概率。库存扣减保留“余额大于等于扣减数”的 SQL 条件，入库保留上限条件。停用商品也锁商品行，与确认流程串行化。

锁定读取必须处于事务中，事务结束后释放；参见 [MySQL 锁定读取文档](https://dev.mysql.com/doc/refman/8.4/en/innodb-locking-reads.html)。Service 的事务方法应通过 Spring 代理调用，同类内部自调用不能当成事务已经生效的证据；参见 [Spring 事务注解文档](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)。

死锁或锁等待超时允许本次请求失败并返回可重试冲突，事务回滚后客户端重试同一单据。第一版不自行实现复杂重试框架；需用独立连接的并发测试证明重复确认和防超扣有效。

## 8. Redis 使用计划

只缓存 `GoodsCatalogVO`：ID、编码、名称、分类、单位、参考价、启停状态，不含 `stockQuantity`。专用资料接口读取它；列表、库存、出入库校验均查询 MySQL。

- Key：`wms:goods:catalog:{id}`；正常 TTL 初始设计为 5 分钟加少量随机抖动。
- 查询：缓存命中返回；未命中读 MySQL，再写缓存；不存在的商品用短期空标记，初始 30 秒。
- 更新：数据库提交后删除资料缓存，失败记录日志并由 TTL 限定陈旧时间。
- 故障：Redis 连接超时或不可用时回源数据库，数据库写入不因删缓存失败而回滚。
- 一致性：这是允许短期陈旧的商品展示数据缓存；提交后删除仍可能与并发旧读交错，不能宣称强一致。TTL 是兜底，不是库存正确性方案。
- 序列化和 TTL 显式配置；不能把客户端库存校验迁到缓存中。

TTL 和缓存配置能力参考 [Spring Data Redis 文档](https://docs.spring.io/spring-data/redis/reference/redis/redis-cache.html)。具体采用手写 Cache-Aside，便于学习读写路径；不同时叠加多套缓存机制。

## 9. 当前风险与演进顺序

1. 当前 POST/PUT 直接绑定 `Goods`，可写入库存、时间等字段。先完成现有修改接口验收，再逐步改为 DTO 和显式字段更新。
2. 当前详情查询不到记录时没有业务层 404；错误结构也未统一。
3. `stockQuantity` 的数据库无符号范围大于 Java Integer，需要补应用边界和数据库约束。
4. 只有上下文测试骨架；编译通过不代表业务或并发正确。
5. 本地配置已被忽略，但没有示例配置和 SQL，复现能力待补。

先补接口边界和可复现基础，再实现单据事务与测试，最后接入 Redis。每项变更只在对应开发任务中进行。
