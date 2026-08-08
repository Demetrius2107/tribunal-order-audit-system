package com.demetrius.tribunal.order.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 合单聚合根（★核心类）。
 *
 * <p>合单是拆单的对称操作：</p>
 * <ul>
 *   <li><b>拆单</b>：1 个父单 → N 个子单（按仓库分）</li>
 *   <li><b>合单</b>：N 个同收货人订单 → 1 个发货单（减少物流成本）</li>
 * </ul>
 *
 * <p>聚合根持有所有成员订单的明细展开（MergeOrderItem），每条明细记录来源订单，
 * 保留订单可追溯性。</p>
 *
 * <p>业务约束：</p>
 * <ol>
 *   <li>至少 2 个成员订单（1 个订单合单没有意义）</li>
 *   <li>所有成员订单 customerId 必须相同（同一收货人）</li>
 *   <li>成员订单状态须为可合单状态（已确认/已转单）</li>
 *   <li>成员订单不可重复</li>
 * </ol>
 */
public class MergeOrder {

    private final String id;

    /** 合单编号（业务唯一键） */
    private final String mergeNo;

    /** 合单客户（所有成员订单共享同一 customerId） */
    private final String customerId;

    /** 成员订单 ID 列表 */
    private final List<String> memberOrderIds;

    /** 合单明细（从成员订单的 OrderSku 展开） */
    private final List<MergeOrderItem> items;

    private MergeOrderStatus status;

    /** 合单运费（合并后重新计算的合并运费，可选） */
    private BigDecimal shippingFee;

    /** 物流单号（发货时填入） */
    private String trackingNo;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private MergeOrder(String id, String mergeNo, String customerId,
                       List<String> memberOrderIds, List<MergeOrderItem> items,
                       MergeOrderStatus status, BigDecimal shippingFee,
                       String trackingNo,
                       LocalDateTime createTime, LocalDateTime updateTime) {
        this.id = id;
        this.mergeNo = mergeNo;
        this.customerId = customerId;
        this.memberOrderIds = memberOrderIds;
        this.items = items;
        this.status = status;
        this.shippingFee = shippingFee;
        this.trackingNo = trackingNo;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    /**
     * 工厂方法：创建合单（初始状态 = CREATED）。
     *
     * <p>校验合单条件后，将每个成员订单的 SKU 展开为合单明细。</p>
     *
     * @param id            合单 ID
     * @param mergeNo       合单编号
     * @param memberOrders  成员订单（至少 2 个）
     * @return 新合单聚合
     * @throws IllegalArgumentException 合单条件不满足
     */
    public static MergeOrder create(String id, String mergeNo, List<Order> memberOrders) {
        Objects.requireNonNull(id, "合单ID不能为空");
        Objects.requireNonNull(mergeNo, "合单编号不能为空");
        if (memberOrders == null || memberOrders.size() < 2) {
            throw new IllegalArgumentException("合单至少需要2个成员订单");
        }

        // 校验：所有订单同一客户 + 状态可合单 + 无重复
        String firstCustomerId = memberOrders.get(0).getCustomerId();
        Set<String> seenOrderIds = new HashSet<>();
        for (Order order : memberOrders) {
            if (!Objects.equals(order.getCustomerId(), firstCustomerId)) {
                throw new IllegalArgumentException(
                        "合单成员订单必须属于同一客户，客户不匹配: " + order.getId());
            }
            if (!isMergeable(order)) {
                throw new IllegalStateException(
                        "订单状态不支持合单: " + order.getId() + " 当前状态: " + order.getStatus());
            }
            if (!seenOrderIds.add(order.getId().value())) {
                throw new IllegalArgumentException("合单成员订单不可重复: " + order.getId());
            }
        }

        // 展开成员订单明细
        List<MergeOrderItem> items = new ArrayList<>();
        for (Order order : memberOrders) {
            for (OrderSku sku : order.getSkus()) {
                items.add(new MergeOrderItem(
                        order.getId().value(),
                        order.getOrderNo(),
                        sku.getSkuCode(),
                        sku.getSkuName(),
                        sku.getQuantity(),
                        sku.getPrice()
                ));
            }
        }

        LocalDateTime now = LocalDateTime.now();
        return new MergeOrder(id, mergeNo, firstCustomerId,
                new ArrayList<>(seenOrderIds), items,
                MergeOrderStatus.CREATED, BigDecimal.ZERO, null, now, now);
    }

    /**
     * 还原工厂方法：从持久化数据完整还原聚合（仓储读取时使用）。
     */
    public static MergeOrder restore(String id, String mergeNo, String customerId,
                                     List<String> memberOrderIds, List<MergeOrderItem> items,
                                     MergeOrderStatus status, BigDecimal shippingFee,
                                     String trackingNo,
                                     LocalDateTime createTime, LocalDateTime updateTime) {
        return new MergeOrder(id, mergeNo, customerId,
                new ArrayList<>(memberOrderIds), new ArrayList<>(items),
                status, shippingFee, trackingNo, createTime, updateTime);
    }

    /** 判断订单是否处于可合单状态（已确认 / 已转单） */
    private static boolean isMergeable(Order order) {
        return order.getStatus() == OrderStatus.CONFIRMED
                || order.getStatus() == OrderStatus.TRANSFERRED;
    }

    // ---------- 状态流转行为 ----------

    /** 打包：CREATED → PACKED */
    public void pack() {
        status.transitionTo(MergeOrderStatus.PACKED);
        this.updateTime = LocalDateTime.now();
    }

    /** 发货：PACKED → SHIPPED，并记录物流单号 */
    public void ship(String trackingNo) {
        if (trackingNo == null || trackingNo.isBlank()) {
            throw new IllegalArgumentException("物流单号不能为空");
        }
        status.transitionTo(MergeOrderStatus.SHIPPED);
        this.trackingNo = trackingNo;
        this.updateTime = LocalDateTime.now();
    }

    /** 送达：SHIPPED → DELIVERED */
    public void deliver() {
        status.transitionTo(MergeOrderStatus.DELIVERED);
        this.updateTime = LocalDateTime.now();
    }

    /** 取消：CREATED → CANCELLED（合单取消后成员订单恢复独立） */
    public void cancel() {
        status.transitionTo(MergeOrderStatus.CANCELLED);
        this.updateTime = LocalDateTime.now();
    }

    /** 设置合单运费（合并后重新计算的合并运费） */
    public void applyShippingFee(BigDecimal fee) {
        if (fee == null || fee.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("合单运费不能为负");
        }
        this.shippingFee = fee;
        this.updateTime = LocalDateTime.now();
    }

    // ---------- 计算属性 ----------

    /** 合单商品总额 = Σ(明细小计) */
    public BigDecimal getTotalAmount() {
        return items.stream()
                .map(MergeOrderItem::subTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 合单明细总行数 */
    public int getItemCount() {
        return items.size();
    }

    // ---------- getters ----------

    public String getId() {
        return id;
    }

    public String getMergeNo() {
        return mergeNo;
    }

    public String getCustomerId() {
        return customerId;
    }

    public List<String> getMemberOrderIds() {
        return Collections.unmodifiableList(memberOrderIds);
    }

    public List<MergeOrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public MergeOrderStatus getStatus() {
        return status;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public String getTrackingNo() {
        return trackingNo;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
}
