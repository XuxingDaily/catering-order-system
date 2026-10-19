# 餐饮服务与订单管理系统

> 基于黑马程序员《苍穹外卖》课程项目进行的二次开发与工程化改进。

本项目并非从零原创，基础业务和初始代码来源于黑马程序员《苍穹外卖》课程项目。在完成课程版本的基础上，对代码结构和订单交易链路进行了进一步重构与完善。

课程基础功能覆盖菜品套餐、购物车、收餐地址、订单履约、营业管理和经营统计；二次开发重点完善了下单幂等、库存原子扣减、订单状态机、支付/退款回调幂等、超时关单和库存补偿，并将原有按技术层组织的代码调整为按业务域聚合的模块化单体结构。当前默认使用 Mock 支付平台在本地复现异步支付及退款回调，同时保留真实微信支付适配实现。

## 项目来源与改进范围

### 课程项目提供的基础能力

- 用户端与管理端基本业务接口
- 员工及微信用户登录、JWT 鉴权
- 分类、菜品、套餐、购物车和收餐地址管理
- 基础订单提交、支付、接单、配送及取消流程
- Redis 缓存、店铺营业状态、WebSocket 通知和 Spring Task
- 工作台、经营数据统计及 Excel 报表导出

本人在学习过程中主要跟随课程进度，并依据 API 文档进行自主编码实现；对于部分重点、难点内容，则结合课程讲解进一步理解和掌握。完成整体学习后，发现项目中仍有一些可以优化和完善的地方，因此在原有基础上进行了部分二次开发与功能改进。

### 本项目的主要改进

- 将 `controller/service/mapper` 全局技术分层重构为按 `catalog/identity/trade/operation` 等业务域组织。
- 将持久层升级为 MyBatis-Plus，同时保留复杂统计查询的 XML SQL。
- 引入业务请求号和数据库唯一约束，完善下单接口幂等处理。
- 增加菜品、套餐库存及数据库原子扣减，补充取消和超时后的库存释放。
- 建立订单状态、业务事件和合法迁移表，通过原状态条件更新控制并发状态流转。
- 拆分订单、库存、支付和退款职责，增加 Mock/微信支付网关适配。
- 完善支付与退款通知的校验、解密及幂等处理。
- 增加可重复执行的数据库迁移脚本及交易核心单元测试。

## 技术栈

- Java 15+（推荐 JDK 17）
- Spring Boot 2.7.3
- MyBatis-Plus 3.5.17、PageHelper
- MySQL、Druid
- Redis、Spring Cache
- JWT、ThreadLocal
- WebSocket、Spring Task
- Knife4j、Apache POI、阿里云 OSS
- JUnit 5、Mockito

## 主要功能

### 用户端

- 微信授权登录与 JWT 身份认证
- 菜品、口味、套餐及店铺营业状态查询
- 购物车增减、查询和清空
- 收餐地址及默认地址管理
- 提交订单、支付、取消订单和再来一单
- 历史订单、订单详情及催单

### 管理端

- 员工登录、账号管理及密码修改
- 分类、菜品、口味和套餐管理
- 菜品、套餐起售与停售
- 店铺营业状态管理
- 订单查询、接单、拒单、取消、配送及完成
- 工作台数据概览
- 营业额、用户、订单和销量 Top 10 统计
- 运营报表 Excel 导出

## 核心设计

### 1. 下单接口幂等

客户端使用 `requestId` 标识一次下单操作，服务端以 `user_id + request_id` 查询并返回已创建订单，数据库唯一索引作为并发场景下的最终防线。

```text
收到下单请求
    ↓
查询 user_id + request_id
    ├─ 已存在：返回原订单
    └─ 不存在：创建订单
                    ↓
              唯一索引冲突
                    ↓
              查询并返回原订单
```

为兼容现有演示页面，请求未携带 `requestId` 时服务端会临时生成 UUID。调用方若要获得完整幂等语义，应在同一次操作及其重试中复用同一个 `requestId`。

### 2. 库存扣减与防超卖

菜品和套餐库存通过数据库条件更新完成原子扣减：

```sql
UPDATE dish
SET stock = stock - #{quantity}
WHERE id = #{dishId}
  AND status = 1
  AND stock >= #{quantity};
```

- 根据受影响行数判断库存是否充足。
- 多商品按照商品 ID 固定顺序扣减，降低并发死锁概率。
- 订单、订单明细、库存扣减和购物车清理处于同一事务。
- 任一商品库存不足时，整笔下单事务回滚。
- 用户取消、商家取消、拒单或超时关单成功后释放库存。
- 订单状态条件更新保证重复取消不会重复释放库存。

当前套餐使用独立库存，不联动扣减套餐内部菜品库存。

### 3. 订单状态机

使用 `OrderStatus + OrderEvent + OrderTransition` 集中描述合法状态迁移：

```text
待付款 ──支付成功──> 待接单 ──商家接单──> 已接单 ──开始配送──> 配送中 ──完成──> 已完成
   │                    │
   ├─用户取消/超时─> 已取消│
   └────────────────────┴─用户取消/商家拒单/管理员取消──> 已取消
```

状态更新将原状态写入 SQL 条件，避免支付、取消、接单和定时任务基于过期状态互相覆盖：

```sql
UPDATE orders
SET status = :targetStatus
WHERE id = :orderId
  AND status = :sourceStatus;
```

### 4. 支付回调幂等

- 支付回调校验 AppID、商户号、支付状态、金额和币种。
- 微信模式校验回调签名、时间戳，并解密 API v3 通知报文。
- 仅允许“待付款 + 未支付”订单原子迁移为“待接单 + 已支付”。
- 保存支付平台 `transactionId`，并通过数据库唯一索引限制重复交易号。
- 重复回调命中相同订单和交易号时直接返回成功。
- 只有第一次状态迁移成功才发送 WebSocket 来单提醒。

### 5. 取消与退款

- 用户取消、管理员取消和商家拒单统一由取消编排服务处理。
- 未支付订单直接取消；已支付订单先进入退款中状态。
- 退款申请使用稳定的商户退款单号，并以唯一索引保证幂等。
- 本地取消事务提交后再调用退款网关，避免外部退款成功而本地事务回滚。
- 退款回调校验订单号、交易号、退款单号、金额及商户信息。
- 重复退款回调不会重复修改订单结果。

### 6. 超时订单自动关闭

- 创建订单时记录 `expire_time`，默认支付有效期为 15 分钟。
- Spring Task 每分钟扫描“待付款 + 未支付 + 已过期”的订单。
- 超时取消与支付回调通过数据库条件更新竞争，只有一个操作能够成功。
- 只有超时取消成功后才释放库存。
- 单笔订单处理失败不会阻断同一批次内其他订单。

当前定时任务完成的是本地订单关闭。真实支付模式下仍应在本地关单前查询并关闭支付平台订单。

### 7. 缓存、认证与通知

- 管理端和用户端使用独立 JWT 配置及拦截路径。
- 登录身份写入 ThreadLocal，并在请求完成后清理。
- 使用 Spring Cache + Redis 缓存高频菜品和套餐查询，管理端变更数据时主动失效缓存。
- 店铺营业状态存储在 Redis。
- 使用 WebSocket 推送新订单及用户催单消息。

## 项目结构

```text
catering-order-system
├─ catering-pojo                   DTO、Entity、VO
├─ catering-server                 Spring Boot 应用及业务实现
│  └─ src/main/java/com/catering
│     ├─ catalog              分类、菜品、套餐、店铺状态
│     ├─ customer             收餐地址
│     ├─ identity             用户、员工、JWT 认证
│     ├─ infrastructure       Redis、Web、OSS、WebSocket 等基础设施
│     ├─ operation            工作台与经营报表
│     ├─ shared               服务端公共结果、异常、上下文及切面
│     └─ trade
│        ├─ cart              购物车
│        ├─ inventory         库存扣减与释放
│        ├─ order             订单、状态机与定时任务
│        └─ payment
│           ├─ pay           支付及支付回调
│           ├─ refund        退款及退款回调
│           └─ infrastructure 支付平台适配
└─ sql                        核心设计增量迁移脚本
```

项目采用模块化单体结构：`catering-server` 同时承载应用入口、公共配置和业务实现，代码按照业务域聚合；`catering-pojo` 独立维护跨业务使用的 DTO、Entity 和 VO。简单模块保留 Controller、Service、Mapper 分层，订单、库存、支付等复杂模块进一步拆分领域状态、应用编排和外部网关。

## 环境要求

- JDK 15 或更高版本，推荐 JDK 17
- Maven 3.8+
- MySQL 5.7/8.0
- Redis 5+
- 可选：Nginx（部署管理端静态资源及反向代理）

默认端口：

| 服务 | 端口 |
|---|---:|
| Spring Boot | 8080 |
| MySQL | 3306 |
| Redis | 6379 |
| Nginx（可选） | 80 |

## 快速开始

### 1. 准备数据库

创建并导入项目基础数据库 `catering_order_system`，然后执行核心设计增量迁移：

```sql
USE catering_order_system;
SOURCE sql/core_design_migration.sql;
```

迁移脚本会补充：

- 菜品、套餐库存字段
- 下单请求号、支付交易号及支付过期时间
- 退款状态、退款单号及退款时间
- 下单幂等、支付、退款唯一索引
- 超时订单和退款任务扫描索引

脚本支持重复执行。执行前仍建议备份数据库，并为历史菜品和套餐设置实际初始库存；迁移默认库存为 `0`。

### 2. 配置 MySQL、Redis 和第三方参数

修改：

```text
catering-server/src/main/resources/application-dev.yml
```

至少需要配置：

```yaml
catering:
  datasource:
    host: localhost
    port: 3306
    database: catering_order_system
    username: your_username
    password: your_password

  redis:
    host: localhost
    port: 6379
    password: your_password
    database: 10
```

请勿将真实数据库密码、微信密钥或 OSS 密钥提交到公开仓库。生产部署应通过环境变量或外部配置中心注入敏感信息。

### 3. 选择支付模式

本地开发默认使用 Mock 模式：

```yaml
catering:
  payment:
    provider: mock
```

Mock 模式会模拟预支付和异步支付/退款回调，不要求微信商户证书。

切换真实微信支付：

```yaml
catering:
  payment:
    provider: wechat
```

同时配置商户号、API v3 Key、商户私钥、平台证书和回调地址。真实模式需要可被微信支付平台访问的 HTTPS 回调地址。

### 4. 编译和测试

```bash
mvn -pl catering-server -am test
```

当前测试覆盖库存聚合与扣减、取消库存释放、Mock 支付/退款网关、支付通知校验和退款结果处理。

### 5. 启动服务

```bash
mvn -pl catering-server -am package -DskipTests
java -jar catering-server/target/catering-server-1.0-SNAPSHOT.jar
```

也可以直接运行：

```text
com.catering.CateringOrderApplication
```

服务启动后访问：

- 后端地址：`http://localhost:8080`
- Knife4j 文档：`http://localhost:8080/doc.html`

## 接口分组

| 前缀 | 用途 |
|---|---|
| `/admin/**` | 管理端接口，登录外需要管理员 JWT |
| `/user/**` | 用户端接口，登录及店铺状态等公开接口除外 |
| `/notify/paySuccess` | 支付结果通知 |
| `/notify/refundResult` | 退款结果通知 |
| `/ws/{sid}` | WebSocket 连接 |

## 测试与并发验收

单元测试：

```bash
mvn -pl catering-server -am test
```

防超卖场景建议使用 JMeter 或数据库集成测试进行验收。例如设置菜品库存为 10，同时发起 100 个购买 1 份的下单请求，预期：

```text
成功订单数 = 10
库存不足数 = 90
最终库存 = 0
成功订单明细数量之和 = 10
失败请求不遗留订单或订单明细
库存始终不小于 0
```

进行接口级并发测试时，应为不同线程准备独立用户和购物车，并使用不同的 `requestId`，避免购物车清空和下单幂等干扰库存测试结果。

建议进一步验证：

- 同一 `requestId` 并发提交只生成一个订单。
- 重复支付回调只触发一次来单通知。
- 支付回调与超时关单并发执行时最终只有一个合法状态。
- 取消接口重复调用不会重复释放库存。
- 多商品订单中任一商品库存不足时整单回滚。

## 当前边界与后续计划

- 演示页面尚未固定传递 `requestId`；调用方需要复用请求号才能获得完整下单幂等语义。
- 默认使用 Mock 支付，真实微信支付适配已预留但需要商户配置和实际联调。
- 超时任务目前关闭本地订单；真实支付模式还需增加支付平台状态查询和关单。
- 退款网关调用失败目前记录日志并保持“退款中”，后续可增加补偿重试与告警任务。
- 可继续补充 MySQL 并发集成测试及 JMeter 测试报告。

