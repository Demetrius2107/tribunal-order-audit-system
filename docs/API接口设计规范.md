# API 接口设计规范

> **文档定位**：定义全系统的 REST API 规范、统一响应格式、错误码体系、各模块接口清单和 Feign 契约。
> **前置文档**：《目标系统架构设计文档》

---

## 一、REST API 通用规范

### 1.1 URL 规范

```
格式：/api/{模块}/{资源}[/{id}][/{动作}]

示例：
  POST   /api/orders                    创建订单
  GET    /api/orders/{orderId}           查询订单详情
  GET    /api/orders                     订单列表（分页）
  PUT    /api/orders/{orderId}           修改订单
  POST   /api/orders/{orderId}/review    审单（动作）
  POST   /api/orders/{orderId}/cancel    取消订单（动作）
```

| 规则 | 说明 |
|------|------|
| 路径全小写 | `/api/orders` 不写 `/api/Orders` |
| 资源名用复数 | `/api/orders` 不写 `/api/order` |
| 动作用动词 | `/review`、`/cancel`、`/ship`、`/sign` |
| ID 用路径参数 | `/api/orders/{orderId}` |
| 过滤用查询参数 | `/api/orders?status=CONFIRMED&page=1&size=20` |
| 版本号 | 当前不加版本前缀，未来需要时加 `/api/v2/` |

### 1.2 HTTP 方法语义

| 方法 | 语义 | 幂等 | 示例 |
|------|------|------|------|
| GET | 查询 | 是 | `GET /api/orders/{id}` |
| POST | 创建/动作 | 否 | `POST /api/orders`、`POST /api/orders/{id}/review` |
| PUT | 全量更新 | 是 | `PUT /api/orders/{id}` |
| PATCH | 部分更新 | 否 | `PATCH /api/orders/{id}/address` |
| DELETE | 删除 | 是 | `DELETE /api/orders/{id}`（逻辑删除） |

### 1.3 分页规范

```
请求参数：
  page    页码，从 1 开始，默认 1
  size    每页条数，默认 20，最大 100
  sort    排序字段，格式：字段名,asc|desc

响应结构：
{
  "success": true,
  "data": {
    "records": [...],
    "total": 150,
    "page": 1,
    "size": 20,
    "totalPages": 8
  }
}
```

### 1.4 认证规范

```
请求头：
  Authorization: Bearer {accessToken}

服务间调用：
  X-Internal-Token: {internalToken}

白名单（无需认证）：
  /api/auth/login
  /api/auth/register
  /api/auth/refresh
  /**/heartbeat
```

---

## 二、统一响应格式

### 2.1 标准响应体

```java
public class ApiResponse<T> {
    private boolean success;    // 是否成功
    private String code;        // 业务码（"200" 成功 / "4xx" 客户端错误 / "5xx" 服务端错误 / "8xxxx" 业务错误）
    private String message;     // 提示消息
    private T data;             // 业务数据
    private String traceId;     // 链路追踪ID
}
```

### 2.2 成功响应

```json
{
  "success": true,
  "code": "200",
  "message": "操作成功",
  "data": {
    "orderId": "1234567890",
    "orderNo": "ORD20260805001",
    "status": "TO_BE_CONFIRMED"
  },
  "traceId": "a1b2c3d4"
}
```

### 2.3 分页响应

```json
{
  "success": true,
  "code": "200",
  "message": "查询成功",
  "data": {
    "records": [
      { "orderId": "1", "orderNo": "ORD001", "status": "CONFIRMED" },
      { "orderId": "2", "orderNo": "ORD002", "status": "SHIPPED" }
    ],
    "total": 150,
    "page": 1,
    "size": 20,
    "totalPages": 8
  }
}
```

### 2.4 错误响应

```json
{
  "success": false,
  "code": "800301",
  "message": "信用额度不足，可用信用：5000.00，应付金额：8000.00",
  "data": null,
  "traceId": "a1b2c3d4"
}
```

---

## 三、错误码体系

### 3.1 错误码编码规则

```
格式：{系统码}{模块码}{错误序号}

系统码：
  4     客户端错误（HTTP 4xx 对应）
  5     服务端错误（HTTP 5xx 对应）
  8     业务逻辑错误（HTTP 200 但业务失败）

模块码：
  0     通用
  1     订单（OMS）
  2     客户/信用
  3     库存
  4     营销/价格
  5     账单（BMS）
  6     结算（FMS）
  7     履约
  8     认证
  9     通知
```

### 3.2 错误码清单

| 错误码 | HTTP | 模块 | 说明 |
|--------|------|------|------|
| **通用** | | | |
| 400 | 400 | 通用 | 请求参数校验失败 |
| 401 | 401 | 通用 | 未登录或 Token 过期 |
| 403 | 403 | 通用 | 无权限执行该操作 |
| 404 | 404 | 通用 | 资源不存在 |
| 429 | 429 | 通用 | 请求过于频繁，请稍后重试 |
| 500 | 500 | 通用 | 系统内部错误 |
| **认证** | | | |
| 800001 | 200 | 认证 | 用户不存在或密码错误 |
| 800002 | 200 | 认证 | 用户名已存在 |
| 800003 | 200 | 认证 | Refresh Token 无效或已过期 |
| 800004 | 200 | 认证 | 登录失败次数过多，账号已锁定 |
| 800005 | 200 | 认证 | Refresh Token 已被吊销 |
| **订单** | | | |
| 800101 | 200 | 订单 | 订单不存在 |
| 800102 | 200 | 订单 | 订单状态非法迁移 |
| 800103 | 200 | 订单 | 订单号已存在（幂等拦截） |
| 800104 | 200 | 订单 | 非整托数量不允许下单 |
| 800105 | 200 | 订单 | 已拼车订单不可关闭 |
| 800106 | 200 | 订单 | 当前状态不允许修改 |
| 800107 | 200 | 订单 | 当前状态不允许取消 |
| **客户/信用** | | | |
| 800201 | 200 | 客户 | 客户不存在 |
| 800202 | 200 | 客户 | 客户已停用 |
| 800301 | 200 | 信用 | 信用额度不足 |
| 800302 | 200 | 信用 | 信用占用失败（并发冲突） |
| **库存** | | | |
| 800401 | 200 | 库存 | SKU 不存在 |
| 800402 | 200 | 库存 | 库存不足，可售量：{qty} |
| 800403 | 200 | 库存 | 库存预占失败（并发冲突） |
| **营销/价格** | | | |
| 800501 | 200 | 营销 | 无有效价格规则 |
| 800502 | 200 | 营销 | 促销规则已失效 |
| 800503 | 200 | 营销 | 折扣超出上限 |
| **账单** | | | |
| 800601 | 200 | 账单 | 账单不存在 |
| 800602 | 200 | 账单 | 账单状态不允许此操作 |
| 800603 | 200 | 账单 | 账单金额与订单不一致 |
| **结算** | | | |
| 800701 | 200 | 结算 | 结算单不存在 |
| 800702 | 200 | 结算 | 重复扣款（幂等拦截） |
| 800703 | 200 | 结算 | 分账比例之和不为 100% |
| 800704 | 200 | 结算 | 退款金额超出原支付金额 |
| **履约** | | | |
| 800801 | 200 | 履约 | 履约单不存在 |
| 800802 | 200 | 履约 | 履约单状态不允许此操作 |

---

## 四、各模块 API 清单

### 4.1 订单中台（OMS）API

#### 订单管理

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/orders` | 创建订单 | order:create |
| GET | `/api/orders` | 订单列表（分页） | order:view |
| GET | `/api/orders/{orderId}` | 订单详情 | order:view |
| PUT | `/api/orders/{orderId}` | 修改订单（审前） | order:modify |
| POST | `/api/orders/{orderId}/review` | 审单（通过/拒绝） | order:review |
| POST | `/api/orders/{orderId}/cancel` | 取消订单 | order:cancel |
| POST | `/api/orders/{orderId}/split` | 拆单 | order:modify |

**创建订单请求体**：
```json
{
  "customerId": "cust-001",
  "channel": "API",
  "skus": [
    {
      "skuCode": "SKU001",
      "quantity": 120,
      "price": 3.50
    }
  ],
  "returnables": [
    {
      "packagingType": "BOTTLE_CRATE",
      "quantity": 10
    }
  ],
  "shippingAddress": {
    "receiverName": "张三",
    "phone": "13800138000",
    "province": "广东省",
    "city": "深圳市",
    "detail": "南山区科技园XX号"
  }
}
```

**审单请求体**：
```json
{
  "approved": true,
  "remark": "审核通过"
}
```

#### 客户信用

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/customers/{customerId}` | 查询客户信息 | customer:credit |
| GET | `/api/customers/{customerId}/credit` | 查询信用额度 | customer:credit |
| POST | `/api/customers` | 创建客户 | customer:credit |
| PUT | `/api/customers/{customerId}` | 修改客户 | customer:credit |

#### 营销价格

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/marketing/price` | 配置价格规则 | marketing:price |
| GET | `/api/marketing/price/{skuCode}` | 查询SKU价格 | marketing:price |
| POST | `/api/marketing/promotions` | 配置促销规则 | marketing:price |
| POST | `/api/marketing/calculate` | 计算订单价格（内部调用） | marketing:price |

### 4.2 库存中心 API

| 方法 | 路径 | 说明 | 调用方 |
|------|------|------|--------|
| POST | `/api/inventory/items` | 新增/更新物料 | 运营/管理员 |
| GET | `/api/inventory/items/{skuCode}` | 查询库存 | OMS (Feign) |
| POST | `/api/inventory/reserve` | 库存预占 | OMS (Feign) |
| POST | `/api/inventory/release` | 库存释放 | OMS (Feign) |
| POST | `/api/inventory/push` | 接收上游推送 | 外部系统 |

**库存预占请求体**：
```json
{
  "orderId": "ord-001",
  "orderNo": "ORD20260805001",
  "items": [
    {
      "skuCode": "SKU001",
      "quantity": 120
    }
  ]
}
```

### 4.3 履约系统 API

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/fulfillments/{id}` | 查询履约单 | fulfillment:ship |
| POST | `/api/fulfillments/{id}/ship` | 发货 | fulfillment:ship |
| POST | `/api/fulfillments/{id}/sign` | 签收 | fulfillment:ship |
| POST | `/api/fulfillments/{id}/cancel` | 取消履约 | fulfillment:ship |

### 4.4 金融结算 API

#### BMS 账单

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/bills/{id}` | 查询账单 | billing:settle |
| GET | `/api/bills` | 账单列表 | billing:settle |
| POST | `/api/bills/{id}/confirm` | 账单审核 | billing:settle |
| POST | `/api/bills/{id}/settle` | 账单结算 | billing:settle |
| POST | `/api/bills/{id}/verify` | 账单核销 | billing:settle |

#### FMS 结算

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/settlements/{id}` | 查询结算单 | finance:settle |
| POST | `/api/settlements/{id}/charge` | 发起扣款 | finance:settle |
| POST | `/api/settlements/{id}/split` | 发起分账 | finance:settle |
| POST | `/api/settlements/refund` | 发起退款 | finance:settle |
| GET | `/api/settlements/{id}/reconcile` | 查询对账结果 | finance:settle |

### 4.5 认证中心 API

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/auth/register` | 注册 | 无 |
| POST | `/api/auth/login` | 登录 | 无 |
| POST | `/api/auth/refresh` | 刷新 Token | 无 |
| POST | `/api/auth/logout` | 登出 | 需登录 |
| GET | `/api/auth/validate` | 校验 Token | 需登录 |
| GET | `/api/auth/permissions` | 查询当前用户权限 | 需登录 |

### 4.6 通知服务 API

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/notifications/send` | 发送通知 | notification:send |
| GET | `/api/notifications` | 通知列表 | 需登录 |
| PUT | `/api/notifications/{id}/read` | 标记已读 | 需登录 |

---

## 五、Feign 接口契约

### 5.1 OMS → 库存中心

```java
@FeignClient(name = "inventory-center", fallback = InventoryCenterClientFallback.class)
public interface InventoryCenterFeignClient {

    @GetMapping("/api/inventory/items/{skuCode}")
    ApiResponse<InventoryItemDto> getBySkuCode(@PathVariable String skuCode);

    @PostMapping("/api/inventory/reserve")
    ApiResponse<Void> reserve(@RequestBody ReserveRequest request);

    @PostMapping("/api/inventory/release")
    ApiResponse<Void> release(@RequestBody ReleaseRequest request);
}
```

### 5.2 FMS → 支付网关（未来）

```java
@FeignClient(name = "payment-gateway", fallback = PaymentGatewayFallback.class)
public interface PaymentGatewayFeignClient {

    @PostMapping("/api/payments/charge")
    ApiResponse<PaymentResultDto> charge(@RequestBody ChargeRequest request);

    @PostMapping("/api/payments/refund")
    ApiResponse<RefundResultDto> refund(@RequestBody RefundRequest request);

    @GetMapping("/api/payments/{paymentId}/status")
    ApiResponse<PaymentStatusDto> queryStatus(@PathVariable String paymentId);
}
```

### 5.3 Feign 统一返回值规范

**所有 Feign 接口必须返回 `ApiResponse<T>`**，不允许直接返回裸对象。

原因：Feign 直接返回裸 DTO 时，如果远程服务返回错误响应体，Feign 会尝试反序列化为 DTO，可能得到空对象或解析异常，无法获取错误信息。

### 5.4 Feign 超时与重试配置

```yaml
feign:
  client:
    config:
      default:
        connect-timeout: 2000    # 连接超时 2s
        read-timeout: 5000       # 读取超时 5s
        logger-level: BASIC
  retry:
    enabled: true
    max-attempts: 3              # 最大重试 3 次
    backoff:
      initial-interval: 1000     # 初始间隔 1s
      multiplier: 2.0            # 退避倍数
      max-interval: 5000         # 最大间隔 5s
```

### 5.5 Feign 降级 Fallback

```java
@Component
public class InventoryCenterClientFallback implements InventoryCenterFeignClient {

    @Override
    public ApiResponse<InventoryItemDto> getBySkuCode(String skuCode) {
        return ApiResponse.fail("800403", "库存服务不可用，请稍后重试");
    }

    @Override
    public ApiResponse<Void> reserve(ReserveRequest request) {
        // 库存预占是核心链路，降级时直接失败
        return ApiResponse.fail("800403", "库存服务不可用，审单失败");
    }

    @Override
    public ApiResponse<Void> release(ReleaseRequest request) {
        // 库存释放降级：记录日志，由对账任务补偿
        log.warn("库存释放降级，订单: {}", request.getOrderId());
        return ApiResponse.success(null);
    }
}
```

---

## 六、Kafka 事件 Topic 规范

### 6.1 Topic 命名

```
格式：tribunal.{系统}.{事件类型}

示例：
  tribunal.order.events          订单事件（OMS 发布）
  tribunal.fulfillment.events    履约事件（Fulfillment 发布）
  tribunal.settlement.events     结算事件（Settlement 发布）
  tribunal.inventory.events      库存事件（Inventory Center 发布）
```

### 6.2 消费者组命名

```
格式：tribunal.{消费方系统}.{用途}

示例：
  tribunal.fulfillment.order-consumer     履约消费订单事件
  tribunal.settlement.order-consumer      结算消费订单事件
  tribunal.oms.fulfillment-consumer       OMS 消费履约事件
  tribunal.oms.settlement-consumer        OMS 消费结算事件
```

### 6.3 死信 Topic

```
格式：{原topic}.DLT

示例：
  tribunal.order.events.DLT
  tribunal.fulfillment.events.DLT
```
