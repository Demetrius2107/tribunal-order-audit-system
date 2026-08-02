# tribunal-order-audit-system — 订单审单系统（DDD + 微服务架构）

> **Author: Demetrius2107**
> **Repository: https://github.com/Demetrius2107/tribunal-order-audit-system.git**
>
> 基于企业级订单自动化系统（OMS 对接 ERP）源码的业务分析，
> 用 **DDD（领域驱动设计）+ 微服务** 从零搭建的订单审单系统。
> 本仓库只提供**架构骨架 + 领域模型 + 接口契约 + 业务 TODO**，
> 具体业务实现需要你对照参考源码分析后逐一完成。

---

## 一、为什么是 DDD + 微服务

参考系统的核心问题：单体服务中巨型类（14000+ 行）、状态机散落、跨域同步调用。
本项目的解法：

| 参考系统痛点 | 本项目解法 |
|---|---|
| 业务规则与技术代码混在一起 | DDD 四层架构，领域层独立，业务规则集中在 domain |
| 状态机散落多处 | 状态机收敛到订单聚合内部（OrderStatus） |
| 数据库操作散落 | 仓储接口（Repository）在 domain 定义，实现放 infrastructure |
| 一个服务塞下所有领域 | **按领域拆分为独立微服务**（订单服务 / 客户服务） |
| 跨域同步调用 | **Feign 跨服务调用**（审单查信用走 customer-service 接口） |

---

## 二、技术栈

| 组件 | 选型 | 说明 |
|---|---|---|
| JDK | 21 | Spring Boot 3 要求 17+ |
| 框架 | Spring Boot 3.2.x | 新技术栈 |
| 微服务 | Spring Cloud 2023.0.x + OpenFeign | 服务间调用（骨架用 url 直连，可升级 Nacos） |
| ORM | MyBatis-Plus 3.5.x | 贴近参考系统习惯 |
| 数据库 | MySQL 8.x（**每服务独立库**） | `sql/customer.sql`、`sql/order.sql` |

---

## 三、微服务架构与模块划分

```
tribunal-order-audit-system（父工程，packaging=pom）
├── tribunal-common/                  # 共享模块（不依赖任何服务）
│   └── com.demetrius.tribunal.common
│       ├── response/ApiResponse      # 统一响应体
│       ├── exception/BizException    # 业务异常
│       └── dto/CustomerCreditDto     # 跨服务 Feign DTO（信用信息）
│
├── tribunal-customer-service/        # ★客户/信用领域微服务（端口 8081）
│   └── com.demetrius.tribunal.customer
│       ├── interfaces/controller/CustomerController   # GET /api/customers/{id}/credit
│       ├── application/service/CustomerApplicationService
│       ├── domain/model/{Customer, CreditLimit}
│       ├── domain/repository/CustomerRepository
│       └── infrastructure/{mapper, model, repository, config}
│
└── tribunal-order-service/           # ★订单/审单领域微服务（端口 8080）
    └── com.demetrius.tribunal.order
        ├── interfaces/{OrderController, dto}
        ├── application/{OrderApplicationService, OrderReviewApplicationService, dto}
        ├── domain/{model(Order/OrderSku/OrderStatus/OrderId), repository, service, event}
        ├── client/CustomerFeignClient      # ★Feign 调用 customer-service 查信用
        └── infrastructure/{mapper, model, repository, config}
```

**依赖规则（不可违反）**：
```
interfaces → application → domain ← infrastructure
domain ← 谁都不能依赖它之外的东西（领域层不 import 任何 Spring/MyBatis 类）
```

**服务间协作（骨架现状）**：

```
tribunal-order-service                    tribunal-customer-service
┌──────────────────────────┐   Feign    ┌──────────────────────────┐
│ OrderReviewApplication  │────────────▶│ CustomerController       │
│  .review()              │  GET /credit│  → CustomerApplicationSvc │
│   └→ CustomerFeignClient│◀────────────│   → CustomerRepository    │
└──────────────────────────┘  信用DTO    └──────────────────────────┘
```

---

## 四、领域模型设计（核心）

### 4.1 订单聚合（Order）—— order-service

```
Order（聚合根）
├── orderId: OrderId           # 值对象
├── orderNo: String            # 订单编号（业务唯一键 → 幂等）
├── customerId: String         # 客户（跨服务引用，order 不存客户数据）
├── status: OrderStatus        # ★状态机
├── skus: List<OrderSku>       # 聚合内实体
├── totalAmount / discountAmount / payableAmount
│
└── 行为（业务规则放在聚合内，而不是 Service 里）
    ├── create()               # 工厂方法：校验 → 初始状态"待确认"
    ├── confirm() / reject()   # 审单通过/拒绝
    ├── startTransfer() / transferSuccess()  # 转单
    ├── ship() / sign() / cancel()
    └── transitTo()            # 统一状态迁移入口（内部走状态机校验）
```

### 4.2 订单状态机（★重点）

```
待确认(TO_BE_CONFIRMED) ──审单通过──▶ 已确认(CONFIRMED)
      │                                │
      │ 审单拒绝                        │ 转单
      ▼                                ▼
   已拒绝(REJECTED)               转单中(TRANSFERRING)
                                      │ 转单成功
                                      ▼
                                   已转单(TRANSFERRED)
                                      │ 发货
                                      ▼
                                   已发货(SHIPPED)
                                      │ 签收
                                      ▼
                                   已签收(SIGNED)
```

**状态机规则**（`OrderStatus` 枚举内实现）：
- 每个状态定义允许迁移的目标状态集合（`TRANSITIONS` 静态块 + EnumMap）
- `canTransitTo(next)` 校验迁移合法性，非法迁移抛异常
- **状态重复回传幂等**：目标状态不在 TRANSITIONS 中 → 拒绝（对应参考系统"状态已同步过"逻辑）
- 实现说明：Java 枚举不能在构造参数中前向引用后续常量（非法前向引用），故用静态块构建

### 4.3 审单领域服务（OrderReviewDomainService）—— order-service

```
审单通过（confirm）：
  ① 状态校验（状态机保证）
  ② ★信用校验：Feign 调 customer-service 获取 CustomerCreditDto，
     校验 可用信用 ≥ 应付金额（跨服务，用 DTO 不用领域对象）
  ③ 整托校验（TODO：SKU 数量整托倍数）
  ④ 促销/折扣重算（TODO）
  ⑤ 审单权限（TODO）
  ⑥ 发布 OrderStatusChangedEvent（订阅者：流水/通知）
```

### 4.4 客户聚合（Customer）—— customer-service

```
Customer
├── customerId / customerCode / name
└── creditLimit: CreditLimit（值对象：limit / used）
    ├── getAvailable() = limit - used
    ├── hasEnoughFor(amount)
    └── occupy() / release()   # TODO：信用占用/释放
```

---

## 五、表结构（微服务独立库）

| 服务 | 库 | 表 | 关键字段 |
|---|---|---|---|
| customer-service | tribunal_customer | t_customer | customer_code(唯一), credit_limit, credit_used |
| order-service | tribunal_order | t_order | **order_no(唯一→幂等)**, customer_id(跨服务), status |
| order-service | tribunal_order | t_order_sku | order_id, sku_code, quantity, price, amount |
| order-service | tribunal_order | t_order_status_record | order_id, from_status, to_status, operator |

---

## 六、本地启动与联调

```bash
# 1. 建库建表（两个独立库）
mysql -uroot -p < sql/customer.sql
mysql -uroot -p < sql/order.sql

# 2. 修改两个服务的 application.yml 数据库账号密码

# 3. 启动 customer-service（端口 8081）
mvn -pl tribunal-customer-service spring-boot:run

# 4. 启动 order-service（端口 8080）
mvn -pl tribunal-order-service spring-boot:run

# 5. 联调验证
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"cust-001","skus":[{"skuCode":"SKU001","skuName":"啤酒","quantity":10,"price":50}]}'
# 下单成功 → 订单状态 TO_BE_CONFIRMED

curl -X POST http://localhost:8080/api/orders/{orderId}/review \
  -H "Content-Type: application/json" \
  -d '{"approved":true,"operator":"admin"}'
# 审单：order-service 通过 Feign 调 customer-service 查信用 → 校验通过 → CONFIRMED
```

---

## 七、学习路线（对照参考源码实现每一个功能）

> 原则：**先读懂参考源码对应逻辑 → 画出你的设计 → 在这个骨架里实现 → 写单元测试**

### 里程碑 1：跑通最小闭环（本周）
- [ ] 建库建表、启动两个服务、Postman 下单 → 查订单状态"待确认"
- [ ] 实现 `OrderApplicationService.createOrder`（对照参考 `saveFinalOrder` + `generateFinalOrder`）
- [ ] 实现订单状态机 `OrderStatus`（对照参考状态流 + 状态前置校验幂等判断）

### 里程碑 2：审单 + 跨服务调用
- [ ] 实现 `OrderReviewApplicationService.review`（对照参考 `orderReview` / `reviewOrder`）
- [ ] 验证 Feign 调用：order-service 审单时正确获取 customer-service 的信用数据
- [ ] 实现 `changeStatus` 统一状态迁移 + 状态流水落库（对照参考 `saveOrderStatusProcessRecordDomain`）

### 里程碑 3：信用 + 业务规则
- [ ] 实现客户信用占用/释放接口（POST /api/customers/{id}/credit/occupy）——对照参考 `creditProcessing`
- [ ] 审单通过后：Feign 调 customer-service 正式扣减信用
- [ ] 整托校验（对照参考 `wholePalletCheck`）

### 里程碑 4：幂等 + 领域事件
- [ ] 下单幂等：`order_no` 唯一键 + 重复提交拦截（对照参考 `@NoRepeatCommit` + 唯一键两层）
- [ ] 用 Spring 事件实现 `OrderStatusChangedEvent` 订阅（通知/审计解耦）

### 里程碑 5（进阶）：微服务治理
- [ ] 接入 Nacos 注册中心（@FeignClient 只写 name，去掉 url 直连）——对照参考 Nacos 配置
- [ ] 引入 RabbitMQ，领域事件改为 MQ 消息（对照参考 MQ 消费者）
- [ ] 跨服务失败处理：熔断/降级（Sentinel 或 Resilience4j）
- [ ] 网关（Spring Cloud Gateway）+ 鉴权过滤器（对照参考网关过滤器）

---

## 八、给学习者的提示

1. **每个 TODO 都对应参考源码的一个类/方法**，文件头注释里标了对照位置
2. **领域层不要 import Spring**：Order/OrderStatus/Customer 是纯 Java 类，
   一旦在 domain 里用了 `@Service`/Mapper，说明分层被破坏了
3. **跨服务边界用 DTO 不用领域对象**：order-service 审单查信用用的是 common 的
   `CustomerCreditDto`（Feign 返回），而不是 customer 的 `Customer` 领域类
4. **状态机是核心中的核心**：花最多时间理解 `OrderStatus`，它对应
   "状态机 = 幂等" 的那层设计
5. **写完一个功能跑一个测试**：给状态机写单元测试（合法迁移/非法迁移/重复状态），
   这是参考系统缺失、你要补上的工程习惯
