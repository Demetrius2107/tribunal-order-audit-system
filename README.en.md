# tribunal-order-audit-system — B2B Full-Chain Order Business System

> **Author: Demetrius2107**
> **Repository: https://github.com/Demetrius2107/tribunal-order-audit-system.git**
>
> A full-chain business system for B2B channel ordering, covering the complete business flow of **Inventory Push → Order Review → Fulfillment → Financial Settlement**:
> Upstream material/inventory push → dealer places order online → salesperson reviews order by rules → finance bill generated and settled → fulfillment and shipment, with statuses fed back end-to-end.
>
> **Tech Stack**: JDK 21 / Spring Boot 3.2.x / Spring Cloud OpenFeign / MyBatis-Plus / MySQL 8.x (one database per service) / Kafka / Redis.
> **Architecture Style**: Domain-Driven Design (DDD) layering + microservices, single repo with multiple modules (3 systems + gateway, 15 Maven modules, including the shared kernel).

---

## 1. Version 0806 Overview

> Snapshot date: 2026-08-05 · Branch: master → `docs/0806-version-overview`

### Implemented (✅)

| Capability | Status | Milestone |
|------|------|--------|
| DDD four-layer architecture (domain layer with zero framework dependency) | ✅ | M0 |
| Order placement + state machine + state transitions log | ✅ | M1 |
| Five-in-one order review orchestration (credit→pricing→inventory→billing→fulfillment→notification) | ✅ | M2 |
| Credit reservation/release loop (reserve on approval, release on rejection/cancel) | ✅ | M2 |
| Finance bills (generate/approve/settle/write-off + status callback driving order state machine) | ✅ | M2 |
| JWT dual Token + RBAC interface-level auth | ✅ | M5 (completed early) |
| Feign retry (Retryer ×3 + spring-retry) | ✅ | M2 |
| Unit tests: 46 cases (order 19 + auth 27) | ✅ | M1/M2 |
| Requirement ID ↔ code implementation mapping table (traceability) | ✅ | - |

### Backlog (⬜ by priority)

| Capability | Milestone | Priority |
|------|--------|--------|
| Kafka events + local message table (async) | M3 | High |
| Order split/merge | M4 | High |
| Warehouse sourcing & allocation | M4 | Medium |
| Promotion/discount/deposit engine (configurable) | M4 | Medium |
| Nacos service discovery + gateway | M5 | Medium |
| Circuit breaker/degradation (Resilience4j) | M5 | Medium |
| Observability (logs/tracing/monitoring/alerting) | M6 | Low |
| Database sharding | M7 | Low |

---

## 2. Current Architecture (15 modules / 3 systems + gateway)

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

- Tech stack: JDK 21 / Spring Boot 3.2.x / Spring Cloud 2023.0.x + OpenFeign / MyBatis-Plus 3.5.7 / MySQL 8.x (one database per service)
- Dependency rule (non-negotiable): `interfaces → application → domain ← infrastructure`; the domain layer must not import any Spring/MyBatis classes
- Inter-service collaboration: synchronous Feign calls (credit/pricing/inventory/billing/fulfillment/notification), direct URLs for now, to be upgraded to Nacos

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

---

## 3. Roadmap

### 3.1 Milestones (M0–M7 per "Production-Grade Requirements Specification")

```
M0 → M1 → M2 → M3 → M4
                ↘ M5 → M6 → M7
```

| Milestone | Topic | Status / Key Actions |
|--------|------|-----------------|
| M0 | Architecture foundation (DDD four layers + unified response/exception) | ✅ Done |
| M1 | Order core loop (place/state machine/transitions) | ✅ Done |
| M2 | Review orchestration + wrap-up (credit/inventory/billing/fulfillment + Feign retry) | ✅ Done |
| M3 | Async & events (Kafka + local message table / inventory flows / payment flows / reconciliation) | ⬜ Next |
| M4 | Business rules engine (promotion/discount/deposit config, order split/merge, warehouse sourcing) | ⬜ |
| M5 | Microservice governance (Nacos / gateway / circuit breaker / Redis idempotency) | 🟡 Auth part completed early |
| M6 | Observability & ops (logs/tracing/monitoring/alerting/CI-CD/load testing) | ⬜ |
| M7 | Production hardening (sharding / archive / DR / security hardening) | ⬜ |

### 3.2 Refactoring Phases (R1–R6 per "Development Plan & Milestones")

```
R1 (merge kernel) → R2 (subdomain merge) → R3 (async refactor) → R4 (business completion)
                                          ↓
                                   R5 (governance) → R6 (observability hardening)
```

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

## 4. Documentation Index (docs/ 18 documents, organized by type)

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
| Scripts | sql/*.sql | 13 DDL scripts (docs/sql/, run in order, see Section 5) |
| Verification | api-test/审单链路验证.http | IDEA HTTP Client flow verification script (docs/api-test/, place→review→bill→fulfillment) |

---

## 5. Local Startup & Integration Testing

```bash
# 1. Create databases and tables (run 13 SQL scripts in order)
for f in docs/sql/*.sql; do mysql -uroot -p < "$f"; done

# 2. Modify database credentials in each service's application.yml

# 3. Start (downstream services first; order-service last)
mvn -pl tribunal-order-auth-service spring-boot:run        # 8087
mvn -pl tribunal-order-customer-service spring-boot:run    # 8081
mvn -pl tribunal-order-inventory-service spring-boot:run   # 8083
mvn -pl tribunal-order-marketing-service spring-boot:run   # 8084
mvn -pl tribunal-order-billing-service spring-boot:run     # 8082
mvn -pl tribunal-order-fulfillment-service spring-boot:run # 8085
mvn -pl tribunal-order-notification-service spring-boot:run# 8086
mvn -pl tribunal-order-task-service spring-boot:run        # 8088
mvn -pl tribunal-order-service spring-boot:run             # 8080 (last, orchestration center)

# 4. Integration verification (suggested cases)
# ① Login to get Token: POST /api/auth/login
# ② Material intake: POST /api/inventory/items
# ③ Price configuration: POST /api/marketing/price
# ④ Place order: POST /api/orders (status=pending confirmation)
# ⑤ Review order: POST /api/orders/{id}/review (triggers five-in-one orchestration)
# ⑥ Settle bill: POST /api/bills/{id}/settle (callbacks to order state machine)
# ⑦ Ship/deliver: POST /api/fulfillments/{id}/ship, /sign
# Full script: docs/api-test/审单链路验证.http
```

---

## 6. Tips for Developers

1. **Follow the map**: every requirement ID (F-/N-/Q-) can be located in the "Requirement ID → Code Implementation Mapping Table" with its code location and implementation status
2. **Do not import Spring in the domain layer**: Order/OrderStatus/FinanceBill are pure Java classes; if `@Service`/Mapper appears, the layering is broken
3. **Use DTOs, not domain objects, across service boundaries**: Feign returns the common DTOs; never pass domain classes across services
4. **The state machine is the core of cores**: order/bill state machine = "state machine + unique key" two-layer idempotency; spend the most time understanding `OrderStatus`
5. **Write a test after each feature**: write unit tests for state machines/amount calculations (valid/invalid/duplicate transitions); the current 46 cases are the baseline
6. **Verify flows with .http scripts**: follow the "Data Flow Verification Guide", use IDEA HTTP Client to play each role, avoiding manual Postman clicks
