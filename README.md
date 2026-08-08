# tribunal-order-audit-system

[![JDK](https://img.shields.io/badge/JDK-21-orange)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.x-green)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.x-green)](https://spring.io/projects/spring-cloud)
[![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.7-blue)](https://baomidou.com/)
[![MySQL](https://img.shields.io/badge/MySQL-8.x-blue)](https://www.mysql.com/)
[![Kafka](https://img.shields.io/badge/Kafka-3.6-black)](https://kafka.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**B2B 订货全链路业务系统** —— 覆盖 **库存推送 → 订单审理 → 履约执行 → 金融结算** 完整业务流。

上游物料/库存推送 → 经销商在线下单 → 销售按规则审单 → 生成金融账单完成财务处理 → 履约发货，状态全程回传。

> **Author: Demetrius2107** · **Repository**: https://github.com/Demetrius2107/tribunal-order-audit-system.git

---

## 功能特性

| 能力 | 说明 |
|------|------|
| 全链路业务闭环 | 库存推送 / 下单 / 审单 / 账单 / 履约 / 结算 / 通知，状态全程回传 |
| 领域驱动设计四层架构 | `interfaces → application → domain ← infrastructure`，domain 层零框架依赖 |
| 审单五合一编排 | 信用校验 → 取价 → 预占库存 → 生成账单 → 创建履约 → 通知，单次编排完成 |
| 订单/账单状态机 | 两层幂等（状态机 + 唯一键），状态流水可追溯 |
| 信用占用/释放闭环 | 审单通过占用，拒绝/取消释放，防超卖防透支 |
| 金融账单全流程 | 生成 / 审核 / 结算 / 核销，回传驱动订单状态机 |
| 统一鉴权 | JWT 双 Token + RBAC 接口级鉴权（M5 提前完成） |
| 服务治理预留 | Feign 重试（Retryer 3 次 + spring-retry）、Nacos / 网关 / 熔断降级 |
| 单仓库多模块 | 3 大系统 + 网关 + 共享内核，15 个 Maven 模块 |
| 可观测性 | 结构化日志 / TraceId 全链路透传 / Prometheus + Grafana 指标监控 |

---

## 快速开始

### 环境要求

| 依赖 | 版本 |
|------|------|
| JDK | 21 |
| Maven | 3.8+ |
| MySQL | 8.x（每服务独立库，共 13 个） |
| Docker（可选） | 中间件编排（MySQL/Nacos/Redis/Kafka/Prometheus/Grafana） |

> 中间件一键启动：`docker-compose up -d`（含 Prometheus 监控）

### 1. 建库建表（13 份 SQL 按序执行）

```bash
for f in docs/sql/*.sql; do mysql -uroot -p < "$f"; done
```

### 2. 修改各服务数据库账号密码

编辑各服务 `src/main/resources/application.yml` 中的 `datasource` 配置。

### 3. 启动服务（依赖下游先行；order-service 最后）

```bash
mvn -pl tribunal-order-auth-service spring-boot:run        # 8087
mvn -pl tribunal-order-customer-service spring-boot:run    # 8081
mvn -pl tribunal-order-inventory-service spring-boot:run   # 8083
mvn -pl tribunal-order-marketing-service spring-boot:run   # 8084
mvn -pl tribunal-order-billing-service spring-boot:run     # 8082
mvn -pl tribunal-order-fulfillment-service spring-boot:run # 8085
mvn -pl tribunal-order-notification-service spring-boot:run# 8086
mvn -pl tribunal-order-task-service spring-boot:run        # 8088
mvn -pl tribunal-order-service spring-boot:run             # 8080（最后，编排中心）
```

---

## 使用示例

完整链路脚本见 `docs/api-test/审单链路验证.http`（IDEA HTTP Client 格式，按顺序点击执行）：

```http
### 登录获取 Token
POST http://localhost:8087/api/auth/login
Content-Type: application/json

{ "username": "dealer001", "password": "123456" }

### 下单
POST http://localhost:8080/api/orders
Content-Type: application/json
Authorization: Bearer <accessToken>

{ "customerId": "cust-001", "items": [{ "skuCode": "SKU-001", "quantity": 10 }] }

### 审单（触发五合一编排）
POST http://localhost:8080/api/orders/{id}/review

### 账单结算（回传订单状态机）
POST http://localhost:8082/api/bills/{id}/settle
```

---

## 架构概览

### 模块结构（15 模块 / 3 大系统 + 网关）

```
tribunal-order-audit-system（父工程，packaging=pom）
│
├── 【共享内核】tribunal-common-*（3 模块，R1 合并产物）
│   ├── tribunal-common-core/     # 核心层：ApiResponse / BizException / 跨服务 DTO（纯 Java，零 Spring 依赖）
│   ├── tribunal-common-starter/  # 起步依赖：JWT 鉴权自动装配 / MyBatis-Plus / Feign 内部 Token
│   └── tribunal-common-event/    # 事件契约层：跨系统领域事件（Kafka 消息体）权威定义
│
├── 【系统一】订单审理系统（9 业务模块）
│   ├── tribunal-order-auth-service/       # 认证授权：登录/注册/双 Token/RBAC   :8087
│   ├── tribunal-order-customer-service/   # 客户信用：客户/信用额度/占用释放   :8081
│   ├── tribunal-order-service/            # 订单审单：下单/状态机/五合一编排   :8080 ★编排中心
│   ├── tribunal-order-inventory-service/  # 库存物料：主数据/预占释放          :8083
│   ├── tribunal-order-marketing-service/  # 营销价格：价格/促销/折扣/押金      :8084
│   ├── tribunal-order-billing-service/    # 金融账单：生成/审核/结算/核销/回传 :8082
│   ├── tribunal-order-fulfillment-service/# 履约执行：出库/发货/签收/发工厂   :8085
│   ├── tribunal-order-notification-service# 通知：站内信/邮件/短信/微信       :8086
│   └── tribunal-order-task-service/       # 定时任务：超时关单/对账/归档       :8088
│
├── 【系统二】库存推送系统（上游数据集成网关）
│   └── tribunal-inventory-push-service/   # 库存推送：主数据/库存推送上游
│
├── 【系统三】金融结算系统（下游资金结算中枢）
│   └── tribunal-finance-settlement-service/  # 金融结算：账单结算/核销/资金流
│
└── 【网关】tribunal-gateway/              # M5：统一入口/路由/Nacos 服务发现/JWT 前置鉴权
```

### 审单五合一编排（order-service 核心链路）

```
审单通过(order-service)
  ① 信用校验   → customer-service（可用信用 ≥ 应付）
  ② 取价校验   → marketing-service（客户价→客户组价→区域价）
  ③ 预占库存   → inventory-service（逐 SKU 预占，防超卖）
  ④ 生成账单   → billing-service（账单 GENERATED，转单）
  ⑤ 创建履约   → fulfillment-service（履约 GENERATED，待出库/发货）
  ⑥ 发送通知   → notification-service（站内信给经销商）
  ⑦ 状态迁移   → order 状态机：待确认 → 已确认
```

**状态回传闭环**：billing 结算/核销 → 回传 order → 订单状态机推进；fulfillment 发货/签收 → 回传 order → 终态。

### 技术栈

JDK 21 / Spring Boot 3.2.x / Spring Cloud 2023.0.x + OpenFeign / MyBatis-Plus 3.5.7 / MySQL 8.x（每服务独立库）/ Kafka / Redis / Nacos / Docker Compose

---

## 路线图

### 里程碑（M0~M7）

| 里程碑 | 主题 | 状态 |
|--------|------|------|
| M0 | 架构底座（领域驱动设计四层 + 统一响应/异常） | ✅ 完成 |
| M1 | 订单核心闭环（下单/状态机/流水） | ✅ 完成 |
| M2 | 审单编排 + 收尾（信用/库存/账单/履约 + Feign 重试） | ✅ 完成 |
| M3 | 异步与事件（Kafka + 本地消息表 / 库存流水 / 收款流水 / 对账） | ⬜ 下一步 |
| M4 | 业务规则引擎（促销/折扣/押金配置化、拆单合单、寻源分仓） | ⬜ |
| M5 | 微服务治理（Nacos / 网关 / 熔断降级 / Redis 幂等） | 🟡 认证部分已提前完成 |
| M6 | 可观测与运维（日志/链路/监控/告警/CI-CD/压测） | ⬜ |
| M7 | 生产加固（分库分表 / 归档 / 容灾 / 安全加固） | ⬜ |

### 重构阶段（R1~R6）

| 阶段 | 主题 | 周期 | 核心目标 |
|------|------|------|----------|
| R1 | 合并共享内核 | 1 周 | 三份 common 合一（order/inventory-push/finance-settlement → tribunal-common-core/starter/event） |
| R2 | OMS 内部子域合并 | 2 周 | customer/inventory/marketing 从独立服务降为 OMS 子模块，Feign → 进程内调用 |
| R3 | 异步事件改造 | 2 周 | billing/fulfillment 从 Feign 同步改 Kafka 异步 + 本地消息表保证原子性 |
| R4 | 业务能力补全 | 3 周 | 拆单/合单/寻源分仓/促销引擎/优惠券/押金引擎/售后退货 |
| R5 | 微服务治理 | 2 周 | Nacos/Resilience4j 熔断/Redis 幂等/Spring Cloud Gateway |
| R6 | 可观测与加固 | 2 周 | 结构化日志/全链路追踪/Prometheus+Grafana/告警/压测 |

> 目标架构：**4 大系统 + 共享内核**（inventory-center 库存中心 / oms 订单中台 / fulfillment 履约 / settlement 金融结算），31 个 Maven 模块，详见《目标系统架构设计文档》《重构架构-模块分包方案》。

---

## 文档索引（docs/ 共 18 份，按类型分目录）

| 分类 | 文档 | 用途 |
|------|------|------|
| 需求 | 需求/需求规格说明书-生产级目标.md | 功能/非功能需求 + 里程碑（M0~M7） |
| 需求 | 需求/需求编号与代码实现映射表.md | 需求编号 → 代码位置 → 实现状态（✅/🟡/⬜） |
| 需求 | 需求/订单业务全系统功能清单.md | 19 系统全景 + 功能清单 |
| 需求 | 需求/金融结算模块需求规格说明书.md | 金融结算模块需求 |
| 需求 | 需求/库存推送模块需求规格说明书.md | 库存推送模块需求 |
| 架构 | 架构/架构总览.md | 当前架构模块全景（15 模块/端口/库） |
| 架构 | 架构/目标系统架构设计文档.md | 目标分层架构 + 4 系统模块划分 |
| 架构 | 架构/重构架构-模块分包方案.md | 31 模块 Maven 树 + 迁移映射 |
| 架构 | 架构/认证授权链路设计.md | JWT + RBAC 全链路 |
| 设计 | 设计/数据库设计文档.md | 全系统表结构 + 索引 + 分片 |
| 设计 | 设计/API接口设计规范.md | REST 规范 + 错误码 + 接口清单 |
| 设计 | 设计/开发规范与工程约定.md | 领域驱动设计分层 + 命名 + Git + 测试 |
| 指南 | 指南/开发计划与里程碑.md | 分阶段重构计划（R1~R6） |
| 指南 | 指南/业务名词与业务处理解析.md | 业务术语字典 |
| 指南 | 指南/OMS核心业务开发指南-拆单寻源状态机.md | 拆单/合单/寻源/状态机补全施工图纸 |
| 指南 | 指南/数据流转验证指南.md | 四层验证策略 + .http 链路脚本 |
| 指南 | 指南/M4业务补全与M6可观测性-开发记录.md | M4 拆单/合单/售后退货 + M6 可观测性落地记录 |
| 指南 | 指南/0806-执行计划.md | 主线开发步骤 + 验收标准（从代码真实状态出发） |
| 脚本 | sql/*.sql | 13 份建库建表脚本（docs/sql/，按序执行，见快速开始） |
| 验证 | api-test/审单链路验证.http | IDEA HTTP Client 链路验证脚本（docs/api-test/，下单→审单→账单→履约） |

---

## 贡献指南

1. **按图索骥**：每个需求编号（F-/N-/Q-）都能在《需求编号与代码实现映射表》里找到代码位置和实现状态
2. **遵守分层约束**：领域层不 import 任何 Spring/MyBatis 类（`interfaces → application → domain ← infrastructure`）
3. **跨服务用 DTO**：Feign 返回用 common 的 DTO，不跨服务传领域对象
4. **写测试**：状态机/金额计算必须有单元测试（合法/非法/重复状态迁移），当前 46 用例是底线
5. **验证链路用 .http 脚本**：按《数据流转验证指南》用 IDEA HTTP Client 扮演各角色，避免手动点 Postman
6. 提交遵循 Conventional Commits（`feat`/`fix`/`docs`/`refactor`/`test`/`build`/`ci`）

---

## License

[MIT](LICENSE) © 2026 Demetrius2107
