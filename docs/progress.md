# 实际开发进度

最后更新日期：2026-10-23。第1节保留2026-10-15的历史核对范围；商品查询与异常响应的后续记录见第10、11节。代码检查、开发者手工反馈与未来计划不可互换。

## 1. 本次核对范围

- 已阅读项目根 `AGENTS.md`，按“开发者亲写、先讲解、一次一个任务、先Review后验收”协作。
- 已检查pom、6个主代码文件、1个测试文件、脱敏本地配置和Git状态。
- 当前Git仓库存在，原有 `AGENTS.md` 已有暂存和未暂存改动；本轮保持原样，不替开发者提交或重置。
- 开始时没有 `docs/`，也没有仓库内可复建SQL；本轮创建了7份规划文档。
- 本次执行 `./mvnw -o compile`，`BUILD SUCCESS`，编译6个主源文件，release 21。本机Java为Temurin 21.0.12.1。
- 文档静态检查通过：7份文档、12处本地链接、连续的T01～T38任务编号，所有任务预计30～90分钟；业务源文件和pom相对HEAD无差异。
- 本次未运行应用、HTTP请求、数据库查询或自动化测试；没有改业务代码、pom、本地配置或数据库。

## 2. 技术与文件现状

| 内容 | 实际状态 | 证据 |
| --- | --- | --- |
| Java 21 / Maven Wrapper | 已有，当前编译通过 | pom + 本次compile |
| Spring Boot 4.0.8 Web MVC | 已有 | pom、启动类、Controller |
| MyBatis-Plus 3.5.17 Boot 4 Starter | 已接入 | pom、BaseMapper/IService/ServiceImpl继承 |
| MySQL数据源配置 | 本地配置存在且Git忽略 | 脱敏配置检查；本次未验证连接 |
| Goods实体 | 已有10个字段 | Goods.java |
| 自增主键映射 | 已有IdType.AUTO | Goods.java；历史ID=2验证 |
| 下划线转驼峰 | 已配置 | application.yaml；历史返回goodsCode等字段 |
| Redis | 未接入 | pom无Redis依赖，配置无Redis项，源码无缓存 |
| 测试 | T02 异常响应测试已加入 | `GlobalExceptionHandlerTest` 1 项、`GlobalExceptionHandlerMvcTest` 7 项；其余业务测试仍待开发 |

## 3. 已完成的基础能力

这里“完成”只指下列明确范围，不表示整个商品模块或仓库MVP已经完成。

| 能力 | 已有证据 | 适用范围 |
| --- | --- | --- |
| Controller→Service→Mapper→MySQL基础分层 | 代码存在；2026-09-29请求返回数据库数据 | 仅商品基础链路 |
| GET /api/goods | 2026-09-29先返回[]，插入后返回商品数组 | 无分页、无目标包装响应 |
| GET /api/goods/{id} | 2026-09-29按实际ID查询成功；2026-10-19新增不存在商品处理，见第10节 | 完整读取契约与自动化验收尚未完成 |
| POST /api/goods | 2026-09-29返回true，并查询到新商品 | 直接接收Entity，输入边界未完善 |
| MySQL自增与MyBatis-Plus映射修正 | 加入IdType.AUTO并重新编译重启后，G002获得id=2 | 不表示当前数据库仍保持同样内容 |
| D00：本次分析与规划文档 | docs七份文件已创建并做静态检查 | 仅文档交付 |

历史服务在2026-09-29结束学习时已停止；本次没有再次检查8080当前运行情况。

## 4. 部分完成的功能

| 功能 | 已有部分 | 缺失部分 |
| --- | --- | --- |
| 修改商品 | PUT方法调用updateById；2026-10-19手工PUT/GET及第二次修改前后对照通过 | 尚无DTO和业务边界；没有自动化测试 |
| 商品模块 | 基础查询/新增链路、修改代码，以及商品不存在的404处理代码 | 校验、VO、包装响应、完整错误契约、唯一冲突处理、启停用、分页、测试 |
| 数据库设计落地 | goods存在历史DDL记录，实体映射存在 | 本次未重新核对实际DDL，仓库缺schema.sql，其余五张表未建 |
| 库存 | goods.stockQuantity可读写 | 尚无受控库存流程、上限保护、期初和库存流水 |
| 项目可复现性 | pom及Wrapper已有 | 无无密码样例、可复建SQL及独立测试库配置 |

## 5. 尚未完成

- 专用请求DTO、响应VO、字段白名单、参数校验。
- 后续业务校验和新错误码的异常映射；T02 约定的公共异常场景已验收，见第11节。
- 商品启停用与分页；库存专用查询。
- 入库主明细、出库主明细、所有单据状态流转。
- 单据确认事务、库存条件更新、行锁、重复确认处理。
- 期初记录、库存流水及对账。
- 数据库约束和索引的版本化交付。
- 业务自动化测试、多明细回滚和真实并发测试。
- Redis资料缓存、TTL、失效和故障回源。
- 登录、权限、多仓、库位、盘点与冲销；这些属于后续扩展，不纳入当前MVP。

## 6. 阶段状态

| 阶段 | 状态 | 判定 |
| --- | --- | --- |
| D00 分析与规划 | 已完成 | 七份文档，未改业务代码 |
| P0 现有PUT验收 | 已完成 | T01手工验收通过；名称和价格改变，商品编码、库存与创建时间在第二次对照中保持不变 |
| P1 商品模块边界 | 部分进行中 | T02 统一响应与异常已验收；T03～T07 尚未整体验收 |
| P2 数据库与测试基础 | 未开始 | 当前只有goods历史结构 |
| P3 入库与流水 | 未开始 | 无对应代码 |
| P4 出库 | 未开始 | 无对应代码 |
| P5 库存查询与对账 | 未开始 | 无对应代码 |
| P6 MVP-B正确性验收 | 未开始 | 无事务/并发测试证据 |
| P7 Redis | 未开始 | 无依赖与实现 |
| P8 项目交付 | 未开始 | 后续依据真实结果验收 |

T01已按现有接口范围完成手工验收；T02已按本任务约定的异常场景完成验收；T03～T38均未标记整项通过。MVP-A的查询、新增和现有修改学习链路已做手工验证；MVP-B尚未形成业务闭环，单据与流水待开发。

## 7. T01：修改商品接口回归（2026-10-19已验收）

任务：[修改商品接口回归](development-plan.md#task-t01)。

业务目的：确认当前PUT确实修改指定商品，只改预期字段。先验收已写方法，再讨论DTO整改，避免重复开发。

开发者操作：

1. 选择实际存在的商品，GET并记录修改前数据，不直接假设ID=2仍存在。
2. 确认运行的是重新编译后的最新服务，记录启动结果。
3. 使用Postman发送PUT `/api/goods`，只传id、goodsName和price。
4. 再GET该商品，对比名称、价格与其他关键字段。
5. 把请求、响应和状态码交给导师Review；通过后再填写下方记录。

验收记录：

- 测试日期：2026-10-19（开发者提供Postman截图）。
- 实际商品ID及原值：ID=1。第一次PUT前的完整商品响应未提供；第一次PUT后的GET作为第二次修改的前值：`goodsName=G0001_1`、`price=19.90`、`goodsCode=G001`、`stockQuantity=10`、`createdAt=2026-09-29T22:12:16`。
- PUT请求/HTTP状态/响应：`PUT /api/goods`，请求体仅含 `id=1`、`goodsName=G0001_1`、`price=19.90`；HTTP 200，响应 `true`。最初误用尾随斜杠 `/api/goods/` 得到路由404，修正URL后成功。
- GET修改后结果：第一次 `GET /api/goods/1` 返回HTTP 200，`goodsName=G0001_1`、`price=19.90`、`updatedAt=2026-10-19T19:08:40`；第二次修改后的GET截图显示 `goodsName=G0001_2`、`price=25.90`、`updatedAt=2026-10-19T19:12:11`。第二次截图未显示HTTP状态码，第二次PUT请求及响应未单独提供，按开发者“ok”反馈记录其操作。
- goodsCode、stockQuantity、createdAt对比：两次GET截图均显示 `goodsCode=G001`、`stockQuantity=10`、`createdAt=2026-09-29T22:12:16`，这三项保持不变。
- Review结论及遗留问题：在现有学习接口范围内，PUT成功、修改值可读回，第二次修改前后受保护字段对照一致，T01手工验收通过。直接绑定Entity、响应为boolean及缺自动化测试留待后续任务处理。
- T01状态：已验收通过（手工）。

2026-10-15规划轮只生成文档，没有代执行写操作；2026-10-19由开发者执行手工写请求并提供截图，T01按现有范围验收完成。

## 8. 已知问题与处理位置

| 问题 | 实际影响 | 对应任务 |
| --- | --- | --- |
| POST/PUT直接绑定Goods | 可写库存、状态、时间等不应由客户端决定的字段 | T04、T05 |
| 查询不存在商品曾返回空 | 2026-10-19增加专用异常与404处理；完整错误契约和自动化测试仍待完成 | T02、T03 |
| boolean写响应 | 无新资源详情及业务错误码 | T02、T04、T05 |
| 现有列表不分页 | 数据增长时返回无界 | T07 |
| UINT数量大于Integer上限 | 读取及运算可能越界 | T11、T16、T24 |
| 现有余额无流水解释 | 不能直接按流水对账 | T18、T29 |
| 无业务测试隔离 | contextLoads不能证明业务正确 | T12、T30～T33 |
| 曾出现旧class继续运行 | 新注解没有加载，产生错误ID策略 | T01运行核对、T37复现说明 |

## 9. 后续更新规范

每完成一个任务追加：实际改动文件、Review问题和解决情况、测试环境/命令/输入、断言结果、日期、遗留项。阶段结束复查各任务的验收标准，再更新阶段状态。

历史记录保留其发生日期；本次源码检查或编译不能替换历史运行结果，也不能把待开发的数据库表或API写成已实现。

## 10. 2026-10-19：商品详情不存在处理阶段性记录

- 已有代码：`GoodsService` 声明按 `Long id` 查询；`GoodsServiceImpl` 查询不到时抛出 `GoodsNotFoundException`；独立的 `GlobalExceptionHandler` 将该异常映射为 HTTP 404，正文取异常消息。`GoodsController` 继续委托 Service。开发者亲自编写业务代码，本次 Code Review 检查通过。
- 编译证据：本地执行 `./mvnw -o -DskipTests compile`，编译8个主源文件，`BUILD SUCCESS`。本次没有运行自动化测试。
- 手工反馈：开发者在重启最新服务后报告，不存在的商品查询返回404；该次404截图文件未能由导师读取，因此按开发者反馈记录。此前曾实测不存在商品返回500，确认普通异常路径已被运行。之后的 `GET /api/goods/1` 截图显示HTTP 200及商品数据，见第7节的PUT回归记录。
- 完成边界：商品不存在的基础404链路已实现并获开发者手工验证。T02所需公共响应、错误码及其他协议异常映射，T03所需VO、包装响应和读取自动化测试仍待实现；T02、T03整项未验收。T01的PUT回归已在第二次修改前后完成字段对照并通过手工验收。

## 11. T02 公共响应和异常基础（2026-10-23 已验收）

2026-10-19 启动 T02 时，`GlobalExceptionHandler` 仅处理商品不存在，返回字符串正文。此后开发者开始建立统一响应结构和业务异常映射；进展见下方记录。T02 启动或完成其中一步均不表示整项已完成。

### 2026-10-22 阶段性验证

- 开发者已将 `GlobalExceptionHandler` 改为统一处理 `BusinessException`：状态码取自异常的 `ErrorCode`，正文使用 `ApiResponse<Void>`；此前商品专用的字符串处理方法已移除。代码位于提交 `f934ce3`，本地 `./mvnw -o -DskipTests compile` 为 `BUILD SUCCESS`。
- 开发者提供的 Postman 响应截图显示 HTTP 404，JSON 正文为 `code=GOODS_NOT_FOUND`、`message=商品不存在`、`data=null`。截图未包含请求 URL，因此这里仅记录响应证据，不补写具体请求路径或 ID。
- 开发者新增 `INVALID_ARGUMENT`（HTTP 400）及 `HttpMessageNotReadableException` 处理方法，返回固定消息“请求体格式错误”；本地 `./mvnw -o -DskipTests compile` 为 `BUILD SUCCESS`。第一次 Postman 截图显示请求路径为 `/api/goods/`，返回框架路由 404，并非 JSON 解析结果；修正为 `/api/goods` 后，开发者反馈结果正确，但本次未收到修正后响应截图，故仅记录为开发者手工反馈。
- 开发者提供 `DELETE /api/goods` 的 Postman 截图：HTTP 405，响应正文为 Spring 默认的 `timestamp/status/error/path` 结构，`path=/api/goods`。请求体仍显示之前测试用的畸形 JSON，但路由方法匹配先返回 405；该截图仅证明不支持的方法路径，尚不证明统一 405 响应已实现。
- 开发者新增 `METHOD_NOT_ALLOWED`（HTTP 405）及 `HttpRequestMethodNotSupportedException` 处理方法；本地 `./mvnw -o -DskipTests compile` 为 `BUILD SUCCESS`。再次提供的 Postman 截图显示 `DELETE /api/goods` 返回 HTTP 405，JSON 正文为 `code=METHOD_NOT_ALLOWED`、`message=请求方法不支持`、`data=null`，405 的统一响应手工验证通过。
- 开发者提供 `POST /api/goods`、`Content-Type: text/plain` 的 Postman 截图：HTTP 415，正文为 Spring 默认的 `timestamp/status/error/path` 结构，`path=/api/goods`。这验证了媒体类型不支持的现状，尚未实现统一 415 响应。
- 结论：商品不存在的统一业务异常响应及 405 响应已获截图验证；畸形 JSON 的 400 处理已编译并由开发者反馈手工通过。T02 的 415、500 等异常映射与针对性测试尚未验收，T02 整项仍进行中。

### 2026-10-23 阶段性验证

- 开发者已编写 415 处理代码与 `INTERNAL_ERROR` 的 500 兜底代码。兜底方法将完整异常写入服务端日志，客户端只返回固定消息；Spring `ErrorResponse` 和类型转换异常由后续异常解析器处理。源码经 Review，`./mvnw -o -DskipTests compile` 为 `BUILD SUCCESS`。415 的统一响应尚无运行截图。
- 开发者新增 `GlobalExceptionHandlerTest.unexpectedExceptionReturnsSafe500`，直接调用处理器，断言 HTTP 500、`INTERNAL_ERROR`、固定消息与空 `data`。开发者反馈 IDE 中测试通过。本地 `./mvnw -o -Dtest=GlobalExceptionHandlerTest test` 已编译两个测试源文件，但 Surefire 3.5.6 的运行依赖未缓存，离线运行在测试执行前失败；因此不把本地 Maven 结果记为测试通过。
- 开发者提供新的 Postman 截图：`POST /api/goods` 返回 HTTP 415，JSON 正文为 `code=UNSUPPORTED_MEDIA_TYPE`、`message=请求内容类型不支持`、`data=null`。截图未显示请求 Header，因此仅据此确认 415 状态与响应正文，不补写本次 Content-Type 的实际值。
- 开发者提供 `POST /api/goods` 的 Postman 截图：Header 显示 `Content-Type: application/json`，响应为 HTTP 400，JSON 正文为 `code=INVALID_ARGUMENT`、`message=请求体格式错误`、`data=null`。截图未展示本次请求 Body；结合开发者反馈按畸形 JSON 场景记录，不补写具体请求内容。
- 开发者提供 `GET /api/goods/abc` 的 Postman 截图：HTTP 400 状态保持正确，但正文为 Spring 默认的 `timestamp/status/error/path`，没有目标 `ApiResponse` 的 `code/message/data`。这说明兜底分支未将类型转换错误误判成 500，但非法路径 ID 的统一响应尚未实现。
- T02 仍进行中：下一步处理路径参数类型转换错误的统一 400 响应；之后检查其余框架请求错误的状态与响应结构，并完成必要的针对性断言。

### 2026-10-23 Postman 复测（导师直接操作）

- 当前运行的本地服务可响应请求。重新发送 `GET /api/goods/999999`，得到 HTTP 404，正文 `GOODS_NOT_FOUND`、`商品不存在`、`data=null`。
- 重新发送 `POST /api/goods`，请求体为不完整 JSON `{"goodsName":`，Header 为 `Content-Type: application/json`；得到 HTTP 400，正文 `INVALID_ARGUMENT`、`请求体格式错误`、`data=null`。
- 重新发送 `DELETE /api/goods`，得到 HTTP 405，正文 `METHOD_NOT_ALLOWED`、`请求方法不支持`、`data=null`。
- 重新发送 `POST /api/goods`，Header 为 `Content-Type: text/plain`；得到 HTTP 415，正文 `UNSUPPORTED_MEDIA_TYPE`、`请求内容类型不支持`、`data=null`。
- 重新发送 `GET /api/goods/abc`，得到 HTTP 400，但正文仍为 Spring 默认的 `timestamp/status/error/path`，不是统一的 `ApiResponse`。因此非法路径 ID 场景未通过 T02 的统一响应要求。
- 尝试在 Postman 新建未知路径请求时，地址输入被客户端错误拼接，未取得可归属指定 URL 的可靠结果；不将这次请求计入验收。现有接口没有安全触发未预料服务异常的 HTTP 入口，因此 500 的 HTTP 层结果也未验证；已有 500 直接调用测试的证据范围保持不变。
- 结论：上述 404、畸形 JSON 400、405、415 均通过本次 HTTP 复测；类型转换 400 仍需开发者修复并复测。T02 保持进行中。

### 2026-10-23 路径参数类型错误复测

- 开发者新增 `MethodArgumentTypeMismatchException` 处理方法，返回 HTTP 400 及统一正文；代码 Review 发现一个未使用的 `MethodArgumentNotValidException` import，需清理。
- 本地执行 `./mvnw -o -DskipTests compile`，`BUILD SUCCESS`。在 8081 临时启动当前编译代码，发送 `GET /api/goods/abc`，实得 HTTP 400，正文 `{"code":"INVALID_ARGUMENT","message":"路径参数格式错误","data":null}`；测试后已停止 8081 服务。
- Postman 对仍运行在 8080 的旧进程复测时，返回原有 Spring 默认 400 正文；该进程尚未加载新代码，不能用其结果否定当前源码。开发者重启日常服务后还需在 8080 回归。
- 此项在当前源码的 HTTP 层已验证通过。T02 整项仍进行中，500 HTTP 层及其他框架请求错误回归未完成。
- 开发者重启 8080 服务后，导师在 Postman 重新发送 `GET /api/goods/abc`：HTTP 400，正文 `{"code":"INVALID_ARGUMENT","message":"路径参数格式错误","data":null}`。日常端口上的统一 400 响应现已通过复测。
- 下一项框架错误核对：导师向 8080 发送 `GET /api/no-such-path`，得到 HTTP 404，但正文仍是 Spring 默认的 `timestamp/status/error/path`。未知接口路径的 HTTP 状态正确，统一响应尚未实现；T02 保持进行中。

### 2026-10-23 未知路径统一 404 验证

- 开发者在 `ErrorCode` 增加 `ROUTE_NOT_FOUND`（HTTP 404），并在 `GlobalExceptionHandler` 增加 `NoResourceFoundException` 处理方法，返回统一的 `ApiResponse<Void>`。本次只做 Review 和验证，未修改业务代码。
- `./mvnw -o -DskipTests compile` 返回 `BUILD SUCCESS`。日常 8080 服务当时未运行；在临时 8081 端口启动当前源码后，发送 `GET /api/no-such-path`，实得 HTTP 404、`Content-Type: application/json`，正文为 `{"code":"ROUTE_NOT_FOUND","message":"接口路径不存在","data":null}`。验证后已停止临时服务。
- Review 遗留：`GlobalExceptionHandler` 中的 `MethodArgumentNotValidException` 和 `Locale` import 未使用，待开发者清理。该请求场景已通过 HTTP 验证；500 的 HTTP 层结果及其他框架请求错误的针对性测试仍未完成，T02 整项保持进行中。

### 2026-10-23 T02 最终验收

- 验收代码：开发者编写的 `ApiResponse`、`ErrorCode`、`BusinessException`、`GlobalExceptionHandler`；本次导师新增仅用于测试的 `GlobalExceptionHandlerMvcTest`，没有修改业务源文件。
- 真实 HTTP 证据：商品不存在 404、未知路径 404、畸形 JSON 400、路径 ID 类型错误 400、方法不支持 405、媒体类型不支持 415 均返回约定的 `code/message/data`。具体请求和结果见本节前述记录。
- 自动化证据：先补齐 Surefire 3.5.6 的本地依赖；`./mvnw -o -Dtest=GlobalExceptionHandlerTest,GlobalExceptionHandlerMvcTest test` 运行 8 项，全部通过。测试覆盖上述业务异常和框架错误的 MVC 状态与错误码，以及未预期异常的 HTTP 500、固定安全消息、空 `data` 和内部细节不外泄；额外验证未单独映射的 406 不会被兜底误判为 500。随后 `./mvnw -o test` 运行全部 9 项测试，失败 0、错误 0、跳过 0，`BUILD SUCCESS`。
- Review 结论：T02 约定的公共异常场景验收通过。`GlobalExceptionHandler` 中两个未使用 import 是非阻塞清理项，待开发者后续清理；406 当前只保证正确状态，不承诺统一错误正文。后续字段校验和新业务错误码随 T03～T07 等对应任务推进，不属于本次 T02 完成范围。

## 12. 当前下一步：T03 商品列表、详情与明确的 404

先解释读取接口的业务逻辑和输出边界，再由开发者编写 `GoodsVO` 与成功响应包装；复用 T02 的商品不存在异常，不重复开发。T03 尚未开始验收。
