package com.demetrius.tribunal.fulfillment.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 履约单聚合根。
 *
 * <p>对应需求：下游履约执行（出库/发货/签收）、发送工厂生产指令。</p>
 *
 * <p>职责：账单结算后创建履约单，执行出库发货、签收，并向工厂发送生产/备货指令。</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>出库扣减库存（调用 inventory-service 释放预占 + 出库）</li>
 *   <li>运单/承运商管理（物流对接）</li>
 *   <li>工厂指令明细（生产批次/备货单）与指令回执</li>
 * </ul>
 */
public class FulfillmentOrder {

    private final FulfillmentId id;

    /** 上游订单编号（关联键） */
    private final String sourceOrderNo;

    private final String customerId;

    private FulfillmentStatus status;

    private final List<FulfillmentLine> lines;

    private BigDecimal totalAmount;

    private LocalDateTime createdAt;

    private LocalDateTime shippedAt;

    private LocalDateTime signedAt;

    private LocalDateTime updateTime;

    private FulfillmentOrder(FulfillmentId id, String sourceOrderNo, String customerId, List<FulfillmentLine> lines) {
        this.id = id;
        this.sourceOrderNo = sourceOrderNo;
        this.customerId = customerId;
        this.lines = lines;
        this.status = FulfillmentStatus.GENERATED;
        this.createdAt = LocalDateTime.now();
        this.updateTime = this.createdAt;
        this.totalAmount = lines.stream()
                .map(FulfillmentLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 工厂方法：账单结算后创建履约单（初始状态 = 已生成）。
     */
    public static FulfillmentOrder create(FulfillmentId id, String sourceOrderNo,
                                          String customerId, List<FulfillmentLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("履约明细不能为空");
        }
        return new FulfillmentOrder(id, sourceOrderNo, customerId, new ArrayList<>(lines));
    }

    /** 发货：已生成 → 已发货（出库完成） */
    public void ship() {
        transitTo(FulfillmentStatus.SHIPPED);
        this.shippedAt = LocalDateTime.now();
        // TODO（学习任务）：出库扣减库存（调用 inventory-service）
    }

    /** 签收：已发货 → 已签收（终态） */
    public void sign() {
        transitTo(FulfillmentStatus.SIGNED);
        this.signedAt = LocalDateTime.now();
    }

    /** 取消：已生成/已发货 → 已取消（终态） */
    public void cancel() {
        transitTo(FulfillmentStatus.CANCELLED);
        // TODO（学习任务）：释放库存占用
    }

    /**
     * 发送工厂生产/备货指令（下单/审单后触发，TODO：对接工厂系统）。
     */
    public void dispatchToFactory() {
        // TODO（学习任务）：生成工厂指令并发送（生产批次/备货单 + 回执）
    }

    private void transitTo(FulfillmentStatus target) {
        if (!status.canTransitTo(target)) {
            throw new IllegalStateException(
                    "非法状态迁移: " + status + " -> " + target);
        }
        this.status = target;
        this.updateTime = LocalDateTime.now();
        // TODO（学习任务）：发布 FulfillmentStatusChangedEvent（由应用层统一发布）
    }

    // ---------- getters ----------

    public FulfillmentId getId() {
        return id;
    }

    public String getSourceOrderNo() {
        return sourceOrderNo;
    }

    public String getCustomerId() {
        return customerId;
    }

    public FulfillmentStatus getStatus() {
        return status;
    }

    public List<FulfillmentLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getShippedAt() {
        return shippedAt;
    }

    public LocalDateTime getSignedAt() {
        return signedAt;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
}
