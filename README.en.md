# tribunal-order-audit-system

[![JDK](https://img.shields.io/badge/JDK-21-orange)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.x-green)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.x-green)](https://spring.io/projects/spring-cloud)
[![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.7-blue)](https://baomidou.com/)
[![MySQL](https://img.shields.io/badge/MySQL-8.x-blue)](https://www.mysql.com/)
[![Kafka](https://img.shields.io/badge/Kafka-3.6-black)](https://kafka.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**B2B Full-Chain Order Business System** — covering the complete business flow of **Inventory Push → Order Review → Fulfillment → Financial Settlement**.

Upstream material/inventory push → dealer places order online → salesperson reviews order by rules → finance bill generated and settled → fulfillment and shipment, with statuses fed back end-to-end.

> **Author: Demetrius2107** · **Repository**: https://github.com/Demetrius2107/tribunal-order-audit-system.git

---

## Features

| Capability | Description |
|------|------|
| Full-chain business loop | Inventory push / order / review / bill / fulfillment / settlement / notification, statuses fed back end-to-end |
| DDD four-layer architecture | `interfaces → application → domain ← infrastructure`, domain layer with zero framework dependency |
| Five-in-one review orchestration | Credit check → pricing → inventory reservation → bill generation → fulfillment creation → notification in one pass |
| Order/bill state machines | Two-layer idempotency (state machine + unique key), traceable state transition logs |
| Credit reservation/release loop | Reserve on approval, release on rejection/cancel, prevents overselling and overdraw |
| Finance bill full flow | Generate / approve / settle / write-off, callbacks driving the order state machine |
| Unified auth | JWT dual Token + RBAC interface-level auth (M5 completed early) |
| Service governance reserved | Feign retry (Retryer ×3 + spring-retry), Nacos / gateway / circuit breaker / degradation |
| Single repo, multiple modules | 3 systems + gateway + shared kernel, 15 Maven modules |
| Observability | Structured logs / TraceId full-link propagation / Prometheus + Grafana metrics |

---

## Quick Start

### Prerequisites

| Dependency | Version |
|------|------|
| JDK | 21 |
| Maven | 3.8+ |
| MySQL | 8.x (one database per service, 13 total) |
| Docker (optional) | Middleware orchestration (MySQL/Nacos/Redis/Kafka/Prometheus/Grafana) |

> One-command middleware startup: `docker-compose up -d` (includes Prometheus monitoring)

### 1. Create databases and tables (run 13 SQL scripts in order)

```bash
for f in docs/sql/*.sql; do mysql -uroot -p < "$f"; done
```

### 2. Configure database credentials per service

Edit the `datasource` section in each service's `src/main/resources/application.yml`.

### 3. Start services (downstream first; order-service last)

```bash
mvn -pl tribunal-order-auth-service spring-boot:run        # 8087
mvn -pl tribunal-order-customer-service spring-boot:run    # 8081
mvn -pl tribunal-order-inventory-service spring-boot:run   # 8083
mvn -pl tribunal-order-marketing-service spring-boot:run   # 8084
mvn -pl tribunal-order-billing-service spring-boot:run     # 8082
mvn -pl tribunal-order-fulfillment-service spring-boot:run # 8085
mvn -pl tribunal-order-notification-service spring-boot:run# 8086
mvn -pl tribunal-order-task-service spring-boot:run        # 8088
mvn -pl tribunal-order-service spring-boot:run             # 8080 (last, orchestration center)
```

---

## Usage Example

Full flow script: `docs/api-test/审单链路验证.http` (IDEA HTTP Client format, click to run in order):

```http
### Login to get Token
POST http://localhost:8087/api/auth/login
Content-Type: application/json

{ "username": "dealer001", "password": "123456" }

### Place order
POST http://localhost:8080/api/orders
Content-Type: application/json
Authorization: Bearer <accessToken>

{ "customerId": "cust-001", "items": [{ "skuCode": "SKU-001", "quantity": 10 }] }

### Review order (triggers five-in-one orchestration)
POST http://localhost:8080/api/orders/{id}/review

### Settle bill (callbacks to order state machine)
POST http://localhost:8082/api/bills/{id}/settle
```

---

## Architecture Overview

### Module Structure (15 modules / 3 systems + gateway)

```
tribunal-order-audit-system (parent project, packaging=pom)
│
├── 【Shared Kernel】tribunal-common-* (3 modules, R1 consolidation result)
│   ├── tribunal-common-core/     # Core layer: ApiResponse / BizException / cross-service DTO (pure Java, zero Spring dependency)
│   ├── tribunal-common-starter/  # Starter: JWT auth auto-configuration / MyBatis-Plus / Feign internal Token
│   └── tribunal-common-event/    # Event contract layer: cross-system domain events (Kafka message body) authoritative definitions
│
├── 【System 1】Order Review System (9 business modules)
│   ├── tribunal-order-auth-service/       # Auth: login/register/dual Token/RBAC   :8087
│   ├── tribunal-order-customer-service/   # Customer credit: customer/credit limit/reserve-release   :8081
│   ├── tribunal-order-service/            # Order review: place/state machine/five-in-one orchestration   :8080 ★orchestration center
│   ├── tribunal-order-inventory-service/  # Inventory: master data/pre-reservation-release          :8083
│   ├── tribunal-order-marketing-service/  # Marketing pricing: price/promotion/discount/deposit      :8084
│   ├── tribunal-order-billing-service/    # Finance bills: generate/approve/settle/write-off/callback :8082
│   ├── tribunal-order-fulfillment-service/# Fulfillment: ship/deliver/sign/factory order             :8085
│   ├── tribunal-order-notification-service# Notification: in-app message/email/SMS/WeChat            :8086
│   └── tribunal-order-task-service/       # Scheduled tasks: timeout close/reconciliation/archive     :8088
│
├── 【System 2】Inventory Push System (upstream data integration gateway)
│   └── tribunal-inventory-push-service/   # Inventory push: master data/push to upstream
│
├── 【System 3】Financial Settlement System (downstream fund settlement hub)
│   └── tribunal-finance-settlement-service/  # Financial settlement: bill settlement/write-off/fund flow
│
└── 【Gateway】tribunal-gateway/              # M5: unified entry/routing/Nacos discovery/JWT pre-auth
```

### Five-in-One Order Review Orchestration (order-service core flow)

```
Order approved (order-service)
  ① Credit check      → customer-service (available credit ≥ amount due)
  ② Pricing check     → marketing-service (customer price→customer group price→regional price)
  ③ Reserve inventory → inventory-service (reserve per SKU, prevent overselling)
  ④ Generate bill     → billing-service (bill GENERATED, hand-off)
  ⑤ Create fulfillment→ fulfillment-service (fulfillment GENERATED, pending ship/deliver)
  ⑥ Send notification → notification-service (in-app message to dealer)
  ⑦ Status transition → order state machine: pending confirmation → confirmed
```

**Status callback loop**: billing settle/write-off → callback to order → order state machine advances; fulfillment ship/deliver → callback to order → terminal state.

### Tech Stack

JDK 21 / Spring Boot 3.2.x / Spring Cloud 2023.0.x + OpenFeign / MyBatis-Plus 3.5.7 / MySQL 8.x (one database per service) / Kafka / Redis / Nacos / Docker Compose

---

## Roadmap

### Milestones (M0–M7)

| Milestone | Topic | Status |
|--------|------|------|
| M0 | Architecture foundation (DDD four layers + unified response/exception) | ✅ Done |
| M1 | Order core loop (place/state machine/transitions) | ✅ Done |
| M2 | Review orchestration + wrap-up (credit/inventory/billing/fulfillment + Feign retry) | ✅ Done |
| M3 | Async & events (Kafka + local message table / inventory flows / payment flows / reconciliation) | ⬜ Next |
| M4 | Business rules engine (promotion/discount/deposit config, order split/merge, warehouse sourcing) | ⬜ |
| M5 | Microservice governance (Nacos / gateway / circuit breaker / Redis idempotency) | 🟡 Auth part completed early |
| M6 | Observability & ops (logs/tracing/monitoring/alerting/CI-CD/load testing) | ⬜ |
| M7 | Production hardening (sharding / archive / DR / security hardening) | ⬜ |

### Refactoring Phases (R1–R6)

| Phase | Topic | Duration | Core Goal |
|------|------|------|----------|
| R1 | Merge shared kernel | 1 week | Consolidate three commons (order/inventory-push/finance-settlement → tribunal-common-core/starter/event) |
| R2 | Merge OMS internal subdomains | 2 weeks | customer/inventory/marketing downgraded from standalone services to OMS submodules, Feign → in-process calls |
| R3 | Async event refactor | 2 weeks | billing/fulfillment from synchronous Feign to Kafka async + local message table for atomicity |
| R4 | Business capability completion | 3 weeks | order split/merge/warehouse sourcing/promotion engine/coupons/deposit engine/after-sales returns |
| R5 | Microservice governance | 2 weeks | Nacos/Resilience4j circuit breaker/Redis idempotency/Spring Cloud Gateway |
| R6 | Observability & hardening | 2 weeks | structured logs/full-link tracing/Prometheus+Grafana/alerting/load testing |

> Target architecture: **4 systems + shared kernel** (inventory-center / oms / fulfillment / settlement), 31 Maven modules. See "Target System Architecture Design" and "Refactoring Architecture — Module Decomposition Plan".

---

## Documentation Index (docs/ 18 documents, organized by type)

| Category | Document | Purpose |
|------|------|------|
| Requirements | 需求/需求规格说明书-生产级目标.md | Functional/non-functional requirements + milestones (M0–M7) |
| Requirements | 需求/需求编号与代码实现映射表.md | Requirement ID → code location → implementation status (✅/🟡/⬜) |
| Requirements | 需求/订单业务全系统功能清单.md | 19-system panorama + feature list |
| Requirements | 需求/金融结算模块需求规格说明书.md | Financial settlement module requirements |
| Requirements | 需求/库存推送模块需求规格说明书.md | Inventory push module requirements |
| Architecture | 架构/架构总览.md | Current architecture module panorama (15 modules/ports/databases) |
| Architecture | 架构/目标系统架构设计文档.md | Target layered architecture + 4-system module split |
| Architecture | 架构/重构架构-模块分包方案.md | 31-module Maven tree + migration mapping |
| Architecture | 架构/认证授权链路设计.md | JWT + RBAC full flow |
| Design | 设计/数据库设计文档.md | Full-system table structures + indexes + sharding |
| Design | 设计/API接口设计规范.md | REST conventions + error codes + API list |
| Design | 设计/开发规范与工程约定.md | DDD layering + naming + Git + testing |
| Guide | 指南/开发计划与里程碑.md | Phased refactoring plan (R1–R6) |
| Guide | 指南/业务名词与业务处理解析.md | Business glossary |
| Guide | 指南/OMS核心业务开发指南-拆单寻源状态机.md | Split/merge/sourcing/state machine construction blueprint |
| Guide | 指南/数据流转验证指南.md | Four-layer verification strategy + .http flow scripts |
| Guide | 指南/M4业务补全与M6可观测性-开发记录.md | M4 split/merge/after-sales + M6 observability implementation log |
| Guide | 指南/0806-执行计划.md | Mainline development steps + acceptance criteria (from actual code state) |
| Scripts | sql/*.sql | 13 DDL scripts (docs/sql/, run in order, see Quick Start) |
| Verification | api-test/审单链路验证.http | IDEA HTTP Client flow verification script (docs/api-test/, place→review→bill→fulfillment) |

---

## Contributing

1. **Follow the map**: every requirement ID (F-/N-/Q-) can be located in the "Requirement ID → Code Implementation Mapping Table" with its code location and implementation status
2. **Respect layering constraints**: the domain layer must not import any Spring/MyBatis classes (`interfaces → application → domain ← infrastructure`)
3. **Use DTOs across service boundaries**: Feign returns the common DTOs; never pass domain classes across services
4. **Write tests**: state machines/amount calculations must have unit tests (valid/invalid/duplicate transitions); the current 46 cases are the baseline
5. **Verify flows with .http scripts**: follow the "Data Flow Verification Guide", use IDEA HTTP Client to play each role, avoiding manual Postman clicks
6. Follow Conventional Commits (`feat`/`fix`/`docs`/`refactor`/`test`/`build`/`ci`)

---

## License

[MIT](LICENSE) © 2026 Demetrius2107
