# OMS 核心业务开发指南：拆单/合单、寻源分仓、状态机补全

> 本文档是你开发三大核心能力的"施工图纸"。每个能力包含：业务概念、规则算法、数据模型变更、代码实现要点、验收标准。
> 开发顺序建议：**状态机补全 → 寻源分仓 → 拆单/合单**（前者是后者的基础）。

---

## 目录

1. [整体流程：三大能力如何串联](#1-整体流程)
2. [能力一：订单状态机补全](#2-订单状态机补全)
3. [能力二：寻源分仓](#3-寻源分仓)
4. [能力三：拆单/合单](#4-拆单合单)
5. [现有代码差距分析](#5-现有代码差距分析)
6. [开发顺序与验收标准](#6-开发顺序与验收标准)

---

## 1. 整体流程

三大能力在审单链路中的位置：

```
客户下单
  │
  ▼
[待确认] TO_BE_CONFIRMED
  │
  ├── 审单通过 ──▶ 重新计价 + 信用校验 + 库存校验
  │                  │
  │                  ▼
  │              【寻源分仓】 ◄── 能力二：决定从哪个仓发
  │                  │
  │                  ▼
  │              【拆单/合单】 ◄── 能力一：按仓库/商家拆成子订单
  │                  │
  │                  ├── 子订单A [已确认] → 转单 → 发货 → 签收
  │                  ├── 子订单B [已确认] → 转单 → 发货 → 签收
  │                  └── 子订单C [已确认] → 转单 → 发货 → 签收
  │                  │
  │                  ▼
  │              父订单状态 = 所有子订单的聚合状态 ◄── 能力三：状态机补全
  │
  └── 审单拒绝 ──▶ [已拒绝] REJECTED
```

### 三个能力的关系

| 关系 | 说明 |
|------|------|
| 寻源 → 拆单 | 寻源决定了每个 SKU 从哪个仓发，拆单按仓库分组生成子订单 |
| 拆单 → 状态机 | 拆单后父订单和子订单各有独立状态机，父订单状态由子订单聚合而来 |
| 状态机 → 寻源 | 状态机需要支持"部分发货""部分签收"，这些只在拆单后才出现 |

**所以开发顺序是：先补全状态机（加状态），再做寻源（加仓库维度），最后做拆单（串联起来）。**

---

## 2. 订单状态机补全

### 2.1 业务概念

你现在的状态机是"单订单全量发货"模型——一个订单要么全发、要么没发。但真实场景中：

- 一个订单拆成 3 个子订单，分别从 3 个仓发货
- 子订单 A 已签收，子订单 B 在途，子订单 C 还没发货
- 父订单状态应该是什么？→ **部分签收**

你需要两组新状态：

| 新状态 | 英文 | 含义 | 出现时机 |
|--------|------|------|----------|
| 拆单中 | `SPLITTING` | 正在执行拆单逻辑 | 寻源完成，开始拆单 |
| 已拆单 | `SPLITTED` | 已拆成多个子订单，父订单成为虚拟订单 | 拆单完成 |
| 部分发货 | `PARTIALLY_SHIPPED` | 多个子订单中部分已发货 | 子订单状态回传 |
| 部分签收 | `PARTIALLY_SIGNED` | 多个子订单中部分已签收 | 子订单状态回传 |

### 2.2 完整状态机（补全后）

```
                          ┌─── REJECTED (终态)
                          │
TO_BE_CONFIRMED ──────────┼─── CANCELLED (终态)
                          │
                          └─── CONFIRMED ──── SPLITTING ──── SPLITTED (父订单终态)
                                                │                  │
                                                │ (未拆单，直接走)   │ (子订单各自独立流转)
                                                ▼                  ▼
                                          TRANSFERRING        子订单: CONFIRMED → TRANSFERRING → TRANSFERRED → SHIPPED → SIGNED
                                                │
                                                ▼
                                          TRANSFERRED
                                                │
                                    ┌───────────┴───────────┐
                                    ▼                       ▼
                              SHIPPED              PARTIALLY_SHIPPED
                                    │                       │
                                    ▼                       ▼
                              SIGNED              PARTIALLY_SIGNED
                                                            │
                                                            ▼
                                                      SIGNED (全部子订单签收)
```

### 2.3 父订单状态聚合规则

父订单状态不是手动设置的，而是**由子订单状态自动推导**：

```java
public OrderStatus aggregateStatus(List<OrderStatus> childStatuses) {
    if (childStatuses.isEmpty()) {
        return this.status; // 没有子订单，保持原状态
    }

    boolean allSigned = childStatuses.stream().allMatch(s -> s == SIGNED);
    boolean allShipped = childStatuses.stream().allMatch(s -> s == SHIPPED || s == SIGNED);
    boolean anyShipped = childStatuses.stream().anyMatch(s -> s == SHIPPED || s == SIGNED);
    boolean anySigned = childStatuses.stream().anyMatch(s -> s == SIGNED);

    if (allSigned) return SIGNED;
    if (anySigned) return PARTIALLY_SIGNED;
    if (allShipped) return SHIPPED;
    if (anyShipped) return PARTIALLY_SHIPPED;
    return this.status; // 没有子订单发货，保持原状态
}
```

### 2.4 数据模型变更

```sql
-- t_order 表新增字段
ALTER TABLE t_order ADD COLUMN parent_order_id VARCHAR(64) NULL COMMENT '父订单ID（拆单后子订单指向父订单）';
ALTER TABLE t_order ADD COLUMN is_split TINYINT DEFAULT 0 COMMENT '是否已拆单：0否 1是';
ALTER TABLE t_order ADD COLUMN split_rule VARCHAR(64) NULL COMMENT '拆单规则：BY_WAREHOUSE/BY_MERCHANT/BY_SHIPPING';
ALTER TABLE t_order ADD COLUMN warehouse_id VARCHAR(64) NULL COMMENT '发货仓库ID（子订单或未拆单订单）';

-- 索引
ALTER TABLE t_order ADD INDEX idx_parent (parent_order_id);
ALTER TABLE t_order ADD INDEX idx_warehouse (warehouse_id);
```

### 2.5 代码实现要点

#### 2.5.1 修改 OrderStatus 枚举

**文件**：`tribunal-order-service/.../domain/model/OrderStatus.java`

```java
public enum OrderStatus {
    TO_BE_CONFIRMED,
    CONFIRMED,
    SPLITTING,        // 新增
    SPLITTED,         // 新增（父订单终态）
    TRANSFERRING,
    TRANSFERRED,
    PARTIALLY_SHIPPED, // 新增
    SHIPPED,
    PARTIALLY_SIGNED,  // 新增
    SIGNED,
    REJECTED,
    CANCELLED,
    PRE_ORDER_ENDED;

    // 状态迁移表更新
    static {
        TRANSITIONS.put(TO_BE_CONFIRMED, EnumSet.of(CONFIRMED, REJECTED, CANCELLED, PRE_ORDER_ENDED));
        TRANSITIONS.put(CONFIRMED, EnumSet.of(SPLITTING, TRANSFERRING, CANCELLED)); // 新增 SPLITTING
        TRANSITIONS.put(SPLITTING, EnumSet.of(SPLITTED, TRANSFERRING));              // 新增：拆单完成或直接走
        TRANSITIONS.put(SPLITTED, EnumSet.noneOf(OrderStatus.class));                // 新增：父订单终态
        TRANSITIONS.put(TRANSFERRING, EnumSet.of(TRANSFERRED, CANCELLED));
        TRANSITIONS.put(TRANSFERRED, EnumSet.of(SHIPPED, PARTIALLY_SHIPPED, CANCELLED)); // 新增 PARTIALLY_SHIPPED
        TRANSITIONS.put(PARTIALLY_SHIPPED, EnumSet.of(SHIPPED, PARTIALLY_SIGNED));       // 新增
        TRANSITIONS.put(SHIPPED, EnumSet.of(SIGNED, PARTIALLY_SIGNED));                   // 新增 PARTIALLY_SIGNED
        TRANSITIONS.put(PARTIALLY_SIGNED, EnumSet.of(SIGNED));                            // 新增
        // 终态不变
        TRANSITIONS.put(SIGNED, EnumSet.noneOf(OrderStatus.class));
        TRANSITIONS.put(REJECTED, EnumSet.noneOf(OrderStatus.class));
        TRANSITIONS.put(CANCELLED, EnumSet.noneOf(OrderStatus.class));
        TRANSITIONS.put(PRE_ORDER_ENDED, EnumSet.noneOf(OrderStatus.class));
    }
}
```

#### 2.5.2 修改 Order 聚合根

**文件**：`tribunal-order-service/.../domain/model/Order.java`

新增字段和方法：

```java
public class Order {
    // ... 现有字段 ...

    // 新增字段
    private OrderId parentOrderId;    // 父订单ID（null 表示是父订单或未拆单订单）
    private boolean isSplit;          // 是否已拆单
    private String splitRule;         // 拆单规则
    private String warehouseId;       // 发货仓库ID

    // 新增方法：开始拆单
    public void startSplit() {
        transitTo(OrderStatus.SPLITTING);
    }

    // 新增方法：拆单完成
    public void completeSplit() {
        transitTo(OrderStatus.SPLITTED);
    }

    // 新增方法：部分发货（子订单状态回传触发）
    public void partialShip() {
        if (this.status == TRANSFERRED) {
            transitTo(OrderStatus.PARTIALLY_SHIPPED);
        } else if (this.status == PARTIALLY_SHIPPED) {
            // 已经是部分发货，保持
        }
    }

    // 新增方法：部分签收
    public void partialSign() {
        if (this.status == SHIPPED || this.status == PARTIALLY_SHIPPED) {
            transitTo(OrderStatus.PARTIALLY_SIGNED);
        }
    }

    // 新增方法：全部签收（聚合子订单状态）
    public void allSigned() {
        transitTo(OrderStatus.SIGNED);
    }

    // 新增方法：聚合子订单状态
    public OrderStatus aggregateChildStatuses(List<OrderStatus> childStatuses) {
        // 见 2.3 节代码
    }

    // 新增方法：创建子订单
    public Order createChildOrder(String warehouseId, List<OrderSku> childSkus, String splitRule) {
        // 子订单继承父订单的客户、类型等信息
        // 子订单的 parentOrderId 指向当前订单
        // 子订单初始状态为 CONFIRMED
        return Order.createChild(this, warehouseId, childSkus, splitRule);
    }

    // 新增工厂方法：创建子订单
    public static Order createChild(Order parent, String warehouseId,
                                     List<OrderSku> childSkus, String splitRule) {
        Order child = new Order(
            OrderId.generate(),
            generateOrderNo(),  // 子订单编号
            parent.getCustomerId(),
            parent.getOrderType(),
            parent.isCarPooling(),
            childSkus,
            parent.getReturnablePackagings(),
            parent.getParentOrderId() == null ? parent.getId() : parent.getParentOrderId()
        );
        child.warehouseId = warehouseId;
        child.splitRule = splitRule;
        child.status = CONFIRMED; // 子订单直接从已确认开始
        return child;
    }
}
```

#### 2.5.3 状态回传处理

当履约系统回传子订单状态时，需要更新父订单：

**文件**：`tribunal-order-service/.../application/service/OrderApplicationService.java`

```java
// 新增方法：处理子订单状态回传
@Transactional
public void handleChildStatusCallback(String childOrderId, OrderStatus childStatus) {
    Order child = orderRepository.findById(new OrderId(childOrderId))
            .orElseThrow(() -> new BizException("子订单不存在"));

    // 1. 更新子订单状态
    child.transitTo(childStatus);
    orderRepository.save(child);

    // 2. 如果有父订单，聚合子订单状态
    if (child.getParentOrderId() != null) {
        Order parent = orderRepository.findById(child.getParentOrderId())
                .orElseThrow(() -> new BizException("父订单不存在"));

        List<Order> siblings = orderRepository.findByParentOrderId(parent.getId());
        List<OrderStatus> siblingStatuses = siblings.stream()
                .map(Order::getStatus)
                .toList();

        OrderStatus aggregated = parent.aggregateChildStatuses(siblingStatuses);
        if (parent.canTransitTo(aggregated)) {
            parent.transitTo(aggregated);
            orderRepository.save(parent);
        }
    }
}
```

#### 2.5.4 Repository 新增方法

```java
public interface OrderRepository {
    // 现有方法...

    // 新增：查询父订单的所有子订单
    List<Order> findByParentOrderId(OrderId parentOrderId);

    // 新增：查询是否已有子订单
    boolean hasChildren(OrderId orderId);
}
```

### 2.6 验收标准

- [ ] OrderStatus 枚举新增 4 个状态，迁移表更新
- [ ] Order 聚合根新增 parentOrderId / isSplit / splitRule / warehouseId 字段
- [ ] 父订单状态聚合逻辑正确：全部签收→SIGNED，部分签收→PARTIALLY_SIGNED，部分发货→PARTIALLY_SHIPPED
- [ ] 子订单状态回传时，父订单状态自动更新
- [ ] 单元测试：状态聚合规则覆盖所有组合
- [ ] 数据库 migration SQL 执行通过

---

## 3. 寻源分仓

### 3.1 业务概念

寻源分仓解决的问题是：**一个订单里的 SKU，应该从哪个仓库发货？**

这不是简单的"哪个仓有货从哪发"——要综合考虑：

| 因素 | 说明 | 优先级 |
|------|------|--------|
| 库存可用性 | 仓库必须有足够的可售库存 | P0（硬性条件） |
| 仓库类型匹配 | 冷链商品只能从冷链仓发，危险品只能从危险品仓发 | P0（硬性条件） |
| 收货地址距离 | 优先选离收货地址最近的仓 | P1 |
| 发货时效 | 仓库的承诺发货时间（24h/48h） | P1 |
| 物流成本 | 从该仓发货的运费 | P2 |
| 仓库负载 | 避免全部订单压到一个仓 | P3 |

### 3.2 寻源算法（简化版，适合学习）

```
输入：List<SkuRequirement>（每个 SKU 的 code + quantity），收货地址 areaCode
输出：Map<WarehouseId, List<SkuRequirement>>（每个仓发哪些 SKU）

算法：
  1. 对每个 SKU，查询所有有足够可售库存的仓库列表
     skuWarehouses = inventoryCenter.getAvailableWarehouses(skuCode, quantity)
     → 返回 List<WarehouseStock{warehouseId, availableQty, warehouseType}>

  2. 过滤仓库类型不匹配的
     if sku 是冷链商品 → 只保留 warehouseType == COLD_CHAIN 的仓
     if sku 是危险品 → 只保留 warehouseType == HAZARDOUS 的仓

  3. 尝试单仓发货（最优解）
     intersection = 所有 SKU 的可用仓库列表取交集
     if intersection 不为空：
         选距离收货地址最近的仓（或成本最低的）
         return {选中仓库: allSkus}
     → 0 次拆单，最优

  4. 单仓不行，求最小拆单
     用贪心算法：按仓库能覆盖的 SKU 数降序排列
     每次选覆盖最多未分配 SKU 的仓库，直到所有 SKU 都分配完
     → 尽量少拆单

  5. 如果有 SKU 在所有仓库都没有足够库存
     → 寻源失败，订单进入"缺货"状态，等待补货或人工处理
```

### 3.3 数据模型变更

#### 3.3.1 库存表增加仓库维度

**你现在的问题**：`t_inventory_item` 只有 `sku_code`，没有 `warehouse_id`。同一个 SKU 在不同仓的库存无法区分。

```sql
-- 修改库存表：增加仓库维度
-- 当前结构：t_inventory_item (sku_code UNIQUE, total_quantity, reserved_quantity)
-- 目标结构：同一个 SKU 可以在多个仓有库存

-- 方案：新建仓库库存表
CREATE TABLE t_warehouse_inventory (
    id VARCHAR(64) PRIMARY KEY,
    sku_code VARCHAR(64) NOT NULL COMMENT 'SKU编码',
    warehouse_id VARCHAR(64) NOT NULL COMMENT '仓库ID',
    warehouse_name VARCHAR(128) COMMENT '仓库名称',
    warehouse_type VARCHAR(32) COMMENT '仓库类型：NORMAL/COLD_CHAIN/HAZARDOUS',
    total_quantity DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '总库存',
    reserved_quantity DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '已预占',
    available_quantity DECIMAL(18,2) GENERATED ALWAYS AS (total_quantity - reserved_quantity) STORED COMMENT '可售库存',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sku_warehouse (sku_code, warehouse_id),
    INDEX idx_sku (sku_code),
    INDEX idx_warehouse (warehouse_id)
) COMMENT '仓库库存表（SKU × 仓库维度）';

-- 仓库主数据表
CREATE TABLE t_warehouse (
    id VARCHAR(64) PRIMARY KEY,
    warehouse_code VARCHAR(64) NOT NULL COMMENT '仓库编码',
    warehouse_name VARCHAR(128) NOT NULL COMMENT '仓库名称',
    warehouse_type VARCHAR(32) NOT NULL COMMENT '类型：NORMAL/COLD_CHAIN/HAZARDOUS',
    province VARCHAR(32) COMMENT '省份',
    city VARCHAR(32) COMMENT '城市',
    district VARCHAR(32) COMMENT '区县',
    address VARCHAR(256) COMMENT '详细地址',
    contact_phone VARCHAR(32) COMMENT '联系电话',
    status VARCHAR(16) DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/DISABLED',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_warehouse_code (warehouse_code)
) COMMENT '仓库主数据';
```

#### 3.3.2 SKU 增加特殊属性

```sql
-- SKU 主数据增加仓库类型要求
-- 这个表在库存中心或商品中心，这里只列出需要的字段
ALTER TABLE t_inventory_item ADD COLUMN storage_type VARCHAR(32) DEFAULT 'NORMAL'
    COMMENT '存储类型：NORMAL/COLD_CHAIN/HAZARDOUS（决定只能发到对应类型仓库）';
```

### 3.4 代码实现要点

#### 3.4.1 新增寻源领域服务

**文件**：`tribunal-order-service/.../domain/service/WarehouseRoutingService.java`

```java
/**
 * 寻源分仓领域服务
 * 输入：订单 SKU 列表 + 收货地址
 * 输出：仓库分配方案（每个 SKU 分配到哪个仓）
 */
public class WarehouseRoutingService {

    /**
     * 寻源分仓核心算法
     *
     * @param skuRequirements 订单 SKU 需求列表
     * @param areaCode        收货地区编码
     * @return 仓库分配方案
     */
    public RoutingResult route(List<SkuRequirement> skuRequirements, String areaCode) {
        // 1. 查询每个 SKU 在各仓库的可售库存
        Map<String, List<WarehouseStock>> skuWarehouseMap = new HashMap<>();
        for (SkuRequirement req : skuRequirements) {
            List<WarehouseStock> stocks = inventoryPort.getAvailableWarehouses(
                req.skuCode(), req.quantity());
            // 过滤仓库类型
            stocks = filterByWarehouseType(req.storageType(), stocks);
            if (stocks.isEmpty()) {
                return RoutingResult.shortage(req.skuCode(), req.quantity());
            }
            skuWarehouseMap.put(req.skuCode(), stocks);
        }

        // 2. 尝试单仓发货
        List<String> allWarehouseIds = skuWarehouseMap.values().stream()
                .flatMap(List::stream)
                .map(WarehouseStock::warehouseId)
                .distinct()
                .toList();

        // 找到能覆盖所有 SKU 的仓库
        List<String> singleWarehouseCandidates = allWarehouseIds.stream()
                .filter(wid -> skuRequirements.stream().allMatch(
                    req -> skuWarehouseMap.get(req.skuCode()).stream()
                        .anyMatch(s -> s.warehouseId().equals(wid))))
                .toList();

        if (!singleWarehouseCandidates.isEmpty()) {
            // 选最优仓（距离最近 / 成本最低）
            String bestWarehouse = selectBestWarehouse(singleWarehouseCandidates, areaCode);
            return RoutingResult.singleWarehouse(bestWarehouse, skuRequirements);
        }

        // 3. 单仓不行，贪心最小拆单
        return greedySplit(skuRequirements, skuWarehouseMap, areaCode);
    }

    /**
     * 贪心拆单：每次选覆盖最多未分配 SKU 的仓库
     */
    private RoutingResult greedySplit(
            List<SkuRequirement> remainingSkus,
            Map<String, List<WarehouseStock>> skuWarehouseMap,
            String areaCode) {

        Map<String, List<SkuRequirement>> warehouseAssignment = new HashMap<>();
        List<SkuRequirement> unassigned = new ArrayList<>(remainingSkus);

        while (!unassigned.isEmpty()) {
            // 统计每个仓能覆盖的未分配 SKU 数
            Map<String, Integer> coverageCount = new HashMap<>();
            for (SkuRequirement req : unassigned) {
                for (WarehouseStock stock : skuWarehouseMap.get(req.skuCode())) {
                    coverageCount.merge(stock.warehouseId(), 1, Integer::sum);
                }
            }

            // 选覆盖最多的仓
            String bestWarehouse = coverageCount.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElseThrow();

            // 把该仓能覆盖的 SKU 分配给它
            List<SkuRequirement> assigned = unassigned.stream()
                    .filter(req -> skuWarehouseMap.get(req.skuCode()).stream()
                        .anyMatch(s -> s.warehouseId().equals(bestWarehouse)))
                    .toList();

            warehouseAssignment.put(bestWarehouse, assigned);
            unassigned.removeAll(assigned);
        }

        return RoutingResult.multiWarehouse(warehouseAssignment);
    }
}
```

#### 3.4.2 寻源结果值对象

```java
/**
 * 寻源结果
 */
public record RoutingResult(
    boolean success,
    boolean singleWarehouse,      // 是否单仓发货
    String shortageSkuCode,       // 缺货 SKU（失败时）
    Map<String, List<SkuRequirement>> warehouseAssignments  // 仓库 → SKU 列表
) {
    public static RoutingResult singleWarehouse(String warehouseId, List<SkuRequirement> skus) {
        return new RoutingResult(true, true, null, Map.of(warehouseId, skus));
    }

    public static RoutingResult multiWarehouse(Map<String, List<SkuRequirement>> assignments) {
        return new RoutingResult(true, false, null, assignments);
    }

    public static RoutingResult shortage(String skuCode, BigDecimal qty) {
        return new RoutingResult(false, false, skuCode, Map.of());
    }

    public boolean needsSplit() {
        return success && !singleWarehouse;
    }
}
```

#### 3.4.3 修改审单流程

**文件**：`tribunal-order-service/.../application/service/OrderReviewApplicationService.java`

在 `approve()` 方法中，信用校验和库存校验之间插入寻源步骤：

```java
public void approve(Order order, String operator) {
    // ① 重新计价（现有逻辑）
    repricing(order);

    // ② 信用校验（现有逻辑）
    validateCredit(order);

    // ③ 库存校验（现有逻辑，改为校验多仓库存）
    validateInventory(order);

    // ③' 【新增】寻源分仓
    List<SkuRequirement> requirements = order.getSkus().stream()
            .map(sku -> new SkuRequirement(sku.getSkuCode(), sku.getQuantity(), sku.getStorageType()))
            .toList();
    RoutingResult routingResult = warehouseRoutingService.route(requirements, order.getAreaCode());

    if (!routingResult.success()) {
        throw new BizException("200020", "寻源失败，SKU[" + routingResult.shortageSkuCode() + "]库存不足");
    }

    // ④ 库存预占（按寻源结果预占）
    if (routingResult.singleWarehouse()) {
        // 单仓：直接预占
        reserveInventoryForWarehouse(order, routingResult.warehouseAssignments());
        order.setWarehouseId(routingResult.warehouseAssignments().keySet().iterator().next());
    } else {
        // 多仓：需要拆单
        order.startSplit();

        // ⑤ 【新增】拆单（见第 4 节）
        List<Order> subOrders = splitOrder(order, routingResult);

        // 预占各子订单的库存
        for (Order subOrder : subOrders) {
            reserveInventoryForWarehouse(subOrder, ...);
        }

        order.completeSplit();
        orderRepository.save(order);
        subOrders.forEach(orderRepository::save);
    }

    // ⑥ 信用占用（现有逻辑）
    occupyCredit(order);

    // ⑦ 生成账单（现有逻辑，改为按子订单生成）
    // ⑧ 创建履约（现有逻辑，改为按子订单创建）
    // ⑨ 通知（现有逻辑）
    // ⑩ 状态迁移 + 事件发布
}
```

### 3.5 验收标准

- [ ] 仓库库存表 `t_warehouse_inventory` 创建，支持同一 SKU 多仓库存
- [ ] 仓库主数据表 `t_warehouse` 创建
- [ ] `WarehouseRoutingService` 实现单仓优先 + 贪心最小拆单算法
- [ ] 单仓场景：所有 SKU 在同一个仓有库存 → 不拆单
- [ ] 多仓场景：SKU 分布在不同仓 → 返回多仓分配方案
- [ ] 缺货场景：某 SKU 所有仓都不够 → 返回寻源失败
- [ ] 寻源结果正确传入拆单逻辑
- [ ] 单元测试：覆盖单仓、多仓、缺货三种场景

---

## 4. 拆单/合单

### 4.1 业务概念

#### 拆单

一个订单包含的 SKU 分属不同仓库（寻源结果为多仓），需要拆成多个子订单，每个子订单独立发货。

```
父订单 ORD-001（3 个 SKU，寻源结果：2 个仓）
  ├── 子订单 ORD-001-1（SKU-A + SKU-B，从北京仓发）
  └── 子订单 ORD-001-2（SKU-C，从上海仓发）
```

#### 拆单维度

| 维度 | 说明 | 例子 |
|------|------|------|
| 按仓库 | 最常见，寻源结果决定 | SKU-A 在北京仓，SKU-B 在上海仓 |
| 按商家 | 不同供应商的货分开 | 供应商甲的货和供应商乙的货 |
| 按配送方式 | 冷链和常温不能混装 | 冷链商品走冷链物流，常温走普通物流 |
| 按时效 | 次日达和三日达分开 | 客户要求部分商品次日达 |

#### 合单

同一个客户的多个订单，如果都从同一个仓发货，可以合并成一个发货单，减少物流成本。

```
订单 ORD-001（北京仓，SKU-A）
订单 ORD-002（北京仓，SKU-B）
  → 合单为发货单 SHIP-001（SKU-A + SKU-B，北京仓）
```

> **注意**：合单是合"发货单"不是合"订单"。订单本身不变，只是在履约环节合并出库。这一步可以先不做，优先做拆单。

### 4.2 拆单规则

```
拆单触发条件：寻源结果为多仓（needsSplit == true）

拆单流程：
  1. 父订单状态 → SPLITTING
  2. 按仓库分组 SKU
  3. 每组创建一个子订单：
     - 继承父订单的客户、类型、收货地址
     - 子订单的 parentOrderId = 父订单 ID
     - 子订单的 warehouseId = 分配的仓库
     - 子订单的 splitRule = "BY_WAREHOUSE"
     - 子订单初始状态 = CONFIRMED
     - 子订单金额 = 按比例分摊（运费/折扣/押金）
  4. 父订单状态 → SPLITTED（成为虚拟订单，不再参与履约）
  5. 子订单各自独立进入后续流程（转单 → 发货 → 签收）
```

### 4.3 金额分摊规则

拆单后金额怎么分？这是个容易被忽略但很重要的点：

```
父订单：
  totalAmount = 1000（3 个 SKU）
  discountAmount = 100
  shippingFee = 50
  payableAmount = 950

子订单 A（2 个 SKU，金额 700）：
  totalAmount = 700
  discountAmount = 70    // 按比例：700/1000 × 100
  shippingFee = 35       // 按比例：700/1000 × 50
  payableAmount = 665

子订单 B（1 个 SKU，金额 300）：
  totalAmount = 300
  discountAmount = 30    // 按比例：300/1000 × 100
  shippingFee = 15       // 按比例：300/1000 × 50
  payableAmount = 285

校验：665 + 285 = 950 ✓
```

分摊公式：

```
子订单折扣 = 父订单折扣 × (子订单商品金额 / 父订单商品金额)
子订单运费 = 父订单运费 × (子订单商品金额 / 父订单商品金额)
子订单押金 = 子订单内空包装押金之和
子订单税费 = 父订单税费 × (子订单商品金额 / 父订单商品金额)
```

> **注意尾差**：最后一个子订单用"父订单总额 - 前面所有子订单之和"来避免精度丢失。

### 4.4 数据模型变更

```sql
-- t_order 已在状态机补全时增加了 parent_order_id / is_split / split_rule / warehouse_id
-- 这里增加子订单明细关联

-- 订单明细增加仓库归属
ALTER TABLE t_order_sku ADD COLUMN warehouse_id VARCHAR(64) NULL
    COMMENT '所属仓库ID（拆单后子订单的明细指向具体仓库）';

-- 拆单记录表（可选，用于审计）
CREATE TABLE t_order_split_record (
    id VARCHAR(64) PRIMARY KEY,
    parent_order_id VARCHAR(64) NOT NULL COMMENT '父订单ID',
    child_order_id VARCHAR(64) NOT NULL COMMENT '子订单ID',
    warehouse_id VARCHAR(64) COMMENT '仓库ID',
    split_rule VARCHAR(64) COMMENT '拆单规则',
    sku_codes TEXT COMMENT '包含的SKU编码（逗号分隔）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_parent (parent_order_id),
    INDEX idx_child (child_order_id)
) COMMENT '拆单记录表';
```

### 4.5 代码实现要点

#### 4.5.1 拆单领域服务

**文件**：`tribunal-order-service/.../domain/service/OrderSplitService.java`

```java
/**
 * 订单拆单领域服务
 */
public class OrderSplitService {

    /**
     * 按寻源结果拆单
     *
     * @param parentOrder    父订单（已确认状态）
     * @param routingResult  寻源结果
     * @return 子订单列表
     */
    public List<Order> splitByWarehouse(Order parentOrder, RoutingResult routingResult) {
        // 1. 父订单进入拆单中
        parentOrder.startSplit();

        // 2. 按仓库分组 SKU
        Map<String, List<OrderSku>> warehouseSkuMap = new HashMap<>();
        for (Map.Entry<String, List<SkuRequirement>> entry : routingResult.warehouseAssignments().entrySet()) {
            String warehouseId = entry.getKey();
            List<OrderSku> warehouseSkus = parentOrder.getSkus().stream()
                    .filter(sku -> entry.getValue().stream()
                        .anyMatch(req -> req.skuCode().equals(sku.getSkuCode())))
                    .map(sku -> sku.withWarehouseId(warehouseId)) // 给 SKU 打上仓库标记
                    .toList();
            warehouseSkuMap.put(warehouseId, warehouseSkus);
        }

        // 3. 创建子订单
        List<Order> subOrders = new ArrayList<>();
        int index = 0;
        BigDecimal totalProductAmount = parentOrder.getTotalAmount();

        for (Map.Entry<String, List<OrderSku>> entry : warehouseSkuMap.entrySet()) {
            String warehouseId = entry.getKey();
            List<OrderSku> childSkus = entry.getValue();

            // 创建子订单
            Order subOrder = Order.createChild(parentOrder, warehouseId, childSkus, "BY_WAREHOUSE");

            // 金额分摊
            BigDecimal childProductAmount = childSkus.stream()
                    .map(OrderSku::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 最后一个子订单用减法避免尾差
            if (index == warehouseSkuMap.size() - 1) {
                applyAmountForLastChild(subOrder, parentOrder, subOrders);
            } else {
                applyProportionalAmount(subOrder, parentOrder, childProductAmount, totalProductAmount);
            }

            subOrders.add(subOrder);
            index++;
        }

        // 4. 父订单拆单完成
        parentOrder.completeSplit();

        return subOrders;
    }

    /**
     * 按比例分摊金额
     */
    private void applyProportionalAmount(Order child, Order parent,
                                          BigDecimal childProductAmount,
                                          BigDecimal totalProductAmount) {
        BigDecimal ratio = childProductAmount.divide(totalProductAmount, 8, RoundingMode.HALF_UP);

        BigDecimal childDiscount = parent.getDiscountAmount().multiply(ratio)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal childShipping = parent.getShippingFee().multiply(ratio)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal childTax = parent.getTaxAmount().multiply(ratio)
                .setScale(2, RoundingMode.HALF_UP);

        child.applyDiscount(childDiscount);
        child.applyShippingFee(childShipping);
        child.applyTax(childTax);

        // 押金按子订单内空包装计算
        BigDecimal childDeposit = child.getReturnablePackagings().stream()
                .map(ReturnablePackaging::getDepositAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        child.applyDeposit(childDeposit);
    }

    /**
     * 最后一个子订单用减法，消除尾差
     */
    private void applyAmountForLastChild(Order lastChild, Order parent, List<Order> previousChildren) {
        BigDecimal allocatedDiscount = previousChildren.stream()
                .map(Order::getDiscountAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal allocatedShipping = previousChildren.stream()
                .map(Order::getShippingFee).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal allocatedTax = previousChildren.stream()
                .map(Order::getTaxAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        lastChild.applyDiscount(parent.getDiscountAmount().subtract(allocatedDiscount));
        lastChild.applyShippingFee(parent.getShippingFee().subtract(allocatedShipping));
        lastChild.applyTax(parent.getTaxAmount().subtract(allocatedTax));

        BigDecimal childDeposit = lastChild.getReturnablePackagings().stream()
                .map(ReturnablePackaging::getDepositAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        lastChild.applyDeposit(childDeposit);
    }
}
```

#### 4.5.2 修改审单流程整合寻源+拆单

**文件**：`tribunal-order-service/.../application/service/OrderReviewApplicationService.java`

完整的 approve 流程（整合寻源 + 拆单）：

```java
public void approve(Order order, String operator) {
    // ① 重新计价
    repricing(order);

    // ② 信用校验
    validateCredit(order);

    // ③ 库存校验（多仓）
    validateMultiWarehouseInventory(order);

    // ④ 寻源分仓
    RoutingResult routingResult = warehouseRoutingService.route(
            toSkuRequirements(order), order.getAreaCode());
    if (!routingResult.success()) {
        throw new BizException("200020", "寻源失败: " + routingResult.shortageSkuCode());
    }

    // ⑤ 判断是否需要拆单
    List<Order> ordersToFulfill;
    if (routingResult.needsSplit()) {
        // 多仓 → 拆单
        List<Order> subOrders = orderSplitService.splitByWarehouse(order, routingResult);
        ordersToFulfill = subOrders;

        // 预占各子订单库存
        for (Order sub : subOrders) {
            reserveInventory(sub);
        }

        // 信用占用按子订单分别占用
        for (Order sub : subOrders) {
            occupyCredit(sub);
        }

        // 保存父订单 + 子订单
        orderRepository.save(order);
        subOrders.forEach(orderRepository::save);
    } else {
        // 单仓 → 不拆单
        String warehouseId = routingResult.warehouseAssignments().keySet().iterator().next();
        order.setWarehouseId(warehouseId);
        ordersToFulfill = List.of(order);

        // 预占库存
        reserveInventory(order);
        // 信用占用
        occupyCredit(order);

        orderRepository.save(order);
    }

    // ⑥ 为每个订单（子订单或主订单）生成账单
    for (Order o : ordersToFulfill) {
        billingFeignClient.transfer(toBillRequest(o));
    }

    // ⑦ 为每个订单创建履约单
    for (Order o : ordersToFulfill) {
        fulfillmentFeignClient.create(toFulfillmentRequest(o));
    }

    // ⑧ 通知
    notificationFeignClient.send(toNotification(order));

    // ⑨ 事件发布
    eventPublisher.publishEvent(new OrderApprovedEvent(order, ordersToFulfill));
}
```

#### 4.5.3 OrderSku 增加仓库归属

```java
public class OrderSku {
    // ... 现有字段 ...

    private String warehouseId; // 新增：该明细归属的仓库

    public OrderSku withWarehouseId(String warehouseId) {
        OrderSku copy = new OrderSku(this.skuCode, this.skuName, this.quantity,
                                      this.price, this.amount);
        copy.warehouseId = warehouseId;
        return copy;
    }
}
```

### 4.6 合单（可选，后续扩展）

合单不在本次开发范围，但设计时需要预留：

```java
/**
 * 合单服务（后续实现）
 * 场景：同一客户的多个订单从同一仓发货，合并成一个发货单
 */
public class OrderMergeService {

    public ShipmentPlan merge(List<Order> ordersFromSameWarehouse) {
        // 1. 校验：必须是同一客户、同一仓库、同一配送方式
        // 2. 合并 SKU 列表
        // 3. 创建合并发货单
        // 4. 各订单状态保持独立，共享一个发货单号
    }
}
```

### 4.7 验收标准

- [ ] `OrderSplitService` 实现按仓库拆单
- [ ] 子订单继承父订单的客户、类型、收货地址
- [ ] 子订单的 parentOrderId 正确指向父订单
- [ ] 金额分摊正确：各子订单应付金额之和 = 父订单应付金额
- [ ] 尾差处理：最后一个子订单用减法
- [ ] 父订单拆单后状态为 SPLITTED，不再参与履约
- [ ] 子订单各自独立进入转单→发货→签收流程
- [ ] 子订单状态回传时，父订单状态自动聚合
- [ ] 单元测试：2 仓拆单、3 仓拆单、金额分摊精度
- [ ] 集成测试：完整审单流程（含寻源+拆单+预占+账单+履约）

---

## 5. 现有代码差距分析

### 5.1 状态机差距

| 项目 | 现有 | 目标 | 差距 |
|------|------|------|------|
| 状态数 | 9 个 | 13 个 | 缺 SPLITTING / SPLITTED / PARTIALLY_SHIPPED / PARTIALLY_SIGNED |
| 子订单概念 | 无 | parentOrderId + 子订单独立状态机 | Order 聚合根需新增字段和工厂方法 |
| 状态聚合 | 无 | 父订单状态由子订单推导 | 需新增 aggregateChildStatuses 方法 |
| 状态回传 | 仅 billing→order 单向 | 子订单→父订单自动聚合 | 需修改 statusCallback 逻辑 |

### 5.2 寻源差距

| 项目 | 现有 | 目标 | 差距 |
|------|------|------|------|
| 库存模型 | sku_code 单维度 | sku_code × warehouse_id 双维度 | 需新建 t_warehouse_inventory 表 |
| 仓库主数据 | 无 | t_warehouse 表 | 需新建 |
| 库存 Feign | `getBySkuCode(skuCode)` | `getAvailableWarehouses(skuCode, quantity)` | 需新增接口 |
| 寻源算法 | 无 | 单仓优先 + 贪心最小拆单 | 需新建 WarehouseRoutingService |
| SKU 存储类型 | 无 | storage_type 字段 | 需新增，用于仓库类型匹配 |

### 5.3 拆单差距

| 项目 | 现有 | 目标 | 差距 |
|------|------|------|------|
| 父子订单关系 | 无 | parentOrderId / isSplit / splitRule | 需新增字段 |
| 金额分摊 | 无 | 按比例分摊 + 尾差处理 | 需新建分摊逻辑 |
| 拆单服务 | 无 | OrderSplitService | 需新建 |
| 审单流程 | 直接 confirm → 后续 | confirm → 寻源 → 拆单 → 子订单各自后续 | 需重构 approve 方法 |
| 账单/履约 | 按主订单生成 | 按子订单分别生成 | 需改为循环调用 |

### 5.4 审单流程改造前后对比

```
改造前（当前）：
  approve()
    → 计价 → 信用校验 → 库存预占(单仓) → 信用占用
    → 生成账单(1个) → 创建履约(1个) → 通知
    → confirm()

改造后（目标）：
  approve()
    → 计价 → 信用校验 → 库存校验(多仓)
    → 【寻源分仓】
    → if 单仓:
        → 库存预占 → 信用占用 → 生成账单(1个) → 创建履约(1个) → confirm()
    → if 多仓:
        → startSplit()
        → 【拆单】生成 N 个子订单
        → 逐子订单: 库存预占 → 信用占用 → 生成账单(N个) → 创建履约(N个)
        → completeSplit() (父订单)
        → 子订单各自 confirm()
```

---

## 6. 开发顺序与验收标准

### 6.1 推荐开发顺序

```
第 1 步：状态机补全（1-2天）
  ├── 修改 OrderStatus 枚举（加 4 个状态）
  ├── 修改 Order 聚合根（加字段 + 方法）
  ├── 修改数据库表（加字段）
  ├── 实现父订单状态聚合逻辑
  └── 写单元测试

第 2 步：库存仓库维度改造（1天）
  ├── 新建 t_warehouse 表
  ├── 新建 t_warehouse_inventory 表
  ├── 修改 InventoryItem 聚合根（加 warehouseId）
  ├── 修改库存 Feign 接口（加 getAvailableWarehouses）
  └── 写测试数据（2 个仓，3 个 SKU）

第 3 步：寻源分仓（2天）
  ├── 实现 WarehouseRoutingService
  ├── 实现单仓优先算法
  ├── 实现贪心最小拆单算法
  ├── 实现 RoutingResult 值对象
  └── 写单元测试（单仓/多仓/缺货三种场景）

第 4 步：拆单/合单（2-3天）
  ├── 实现 OrderSplitService
  ├── 实现金额分摊逻辑（含尾差处理）
  ├── 修改 OrderSku（加 warehouseId）
  ├── 重构 approve 方法（整合寻源+拆单）
  ├── 修改账单/履约生成（按子订单循环）
  └── 写集成测试

第 5 步：状态回传联动（1天）
  ├── 修改 statusCallback 处理子订单状态
  ├── 实现父订单状态自动聚合
  └── 写测试
```

### 6.2 总验收清单

| 序号 | 验收项 | 通过标准 |
|------|--------|----------|
| 1 | 单仓审单 | 订单审单通过，寻源为单仓，不拆单，直接进入履约 |
| 2 | 多仓审单 | 订单审单通过，寻源为多仓，拆成 N 个子订单，各子订单独立履约 |
| 3 | 缺货拦截 | 某 SKU 所有仓都不够，审单失败，返回缺货 SKU 编码 |
| 4 | 金额分摊 | 各子订单应付金额之和 = 父订单应付金额，精度 2 位 |
| 5 | 父订单状态 | 父订单状态 = SPLITTED，不再参与履约 |
| 6 | 子订单状态 | 子订单各自独立流转：CONFIRMED → TRANSFERRING → TRANSFERRED → SHIPPED → SIGNED |
| 7 | 部分发货 | 3 个子订单，1 个已发货 → 父订单 = PARTIALLY_SHIPPED |
| 8 | 部分签收 | 3 个子订单，1 个已签收 → 父订单 = PARTIALLY_SIGNED |
| 9 | 全部签收 | 所有子订单签收 → 父订单 = SIGNED |
| 10 | 状态机幂等 | 重复状态回传不会触发非法迁移 |

### 6.3 涉及文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `OrderStatus.java` | 修改 | 新增 4 个状态 + 迁移规则 |
| `Order.java` | 修改 | 新增字段 + 方法 + 子订单工厂 |
| `OrderSku.java` | 修改 | 新增 warehouseId + withWarehouseId |
| `OrderRepository.java` | 修改 | 新增 findByParentOrderId |
| `WarehouseRoutingService.java` | 新建 | 寻源分仓领域服务 |
| `OrderSplitService.java` | 新建 | 拆单领域服务 |
| `RoutingResult.java` | 新建 | 寻源结果值对象 |
| `SkuRequirement.java` | 新建 | SKU 需求值对象 |
| `OrderReviewApplicationService.java` | 修改 | 重构 approve 方法 |
| `OrderApplicationService.java` | 修改 | 新增状态回传处理 |
| `InventoryFeignClient.java` | 修改 | 新增 getAvailableWarehouses |
| `sql/order.sql` | 修改 | 新增字段 |
| `sql/inventory.sql` | 修改 | 新建仓库库存表 |
| 测试文件 | 新建 | 状态聚合/寻源/拆单/金额分摊 |

---

## 附录：关键设计决策

### A1. 为什么父订单不直接删除，而是标记为 SPLITTED？

父订单是客户看到的"主订单"，客户不关心你后面拆成了几个子订单。查询订单列表时展示父订单，点进去看详情才展开子订单。如果删掉父订单，客户的订单就"消失"了。

### A2. 为什么金额分摊用比例而不是平均分？

折扣和运费通常和商品金额正相关（满 1000 减 100），按比例分摊最合理。平均分会导致子订单 A 的应付金额和实际商品金额不匹配，影响财务对账。

### A3. 为什么贪心算法够用，不需要最优解？

最优拆单（最少仓数）是 NP-hard 问题（集合覆盖问题）。实际订单的 SKU 数通常不超过 50 个、仓库数不超过 10 个，贪心算法的结果和最优解差距很小，性能可接受。京东也是用贪心 + 人工规则修正。

### A4. 拆单时机：审单通过时 vs 审单通过后？

推荐在审单通过时同步拆单。原因：
- 拆单结果影响库存预占（每个子订单预占不同仓的库存）
- 拆单结果影响账单生成（每个子订单生成独立账单）
- 如果异步拆单，审单返回后客户看到的还是一个未拆的订单，体验不好

### A5. 合单为什么先不做？

合单涉及发货单层面的合并，需要改履约系统的出库逻辑，复杂度高。而且合单的收益（省运费）在 B2B 场景下不如 B2C 明显——B2B 订单通常是大批量单 SKU，不太会出现"同客户同仓多订单"的情况。先把拆单做对，合单后续按需扩展。
