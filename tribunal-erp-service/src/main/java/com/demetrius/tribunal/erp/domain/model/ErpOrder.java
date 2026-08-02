package com.demetrius.tribunal.erp.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ERP 履约订单聚合根。
 *
 * <p>对应需求：F-307（接收转单）、F-503（发货/签收回传）、N-304（状态回传幂等）。</p>
 *
 * <p>职责：ERP 侧对 OMS 转单的履约生命周期——接收、发货、签收、关闭、取消。
 * 业务规则（状态机校验、金额核对）内聚在聚合内部。</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>库存锁定/释放：接收转单时锁定库存，签收/取消时释放（对应 F-502）</li>
 *   <li>部分发货/部分签收：明细级数量跟踪</li>
 *   <li>与 OMS 订单金额/状态对账（对应 F-701）</li>
 * </ul>
 */
public class ErpOrder {

    private final ErpOrderId id;

    /** 上游 OMS 订单编号（业务关联键，回传状态时携带） */
    private final String sourceOrderNo;

    private final String customerId;

    private ErpOrderStatus status;

    private final List<ErpOrderLine> lines;

    private BigDecimal totalAmount;

    private LocalDateTime receivedAt;

    private LocalDateTime shippedAt;

    private LocalDateTime signedAt;

    private LocalDateTime updateTime;

    private ErpOrder(ErpOrderId id, String sourceOrderNo, String customerId, List<ErpOrderLine> lines) {
        this.id = id;
        this.sourceOrderNo = sourceOrderNo;
        this.customerId = customerId;
        this.lines = lines;
        this.status = ErpOrderStatus.RECEIVED;
        this.receivedAt = LocalDateTime.now();
        this.updateTime = this.receivedAt;
        this.totalAmount = lines.stream()
                .map(ErpOrderLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 工厂方法：接收 OMS 转单，创建履约单（初始状态 = 已接收）。
     */
    public static ErpOrder receive(ErpOrderId id, String sourceOrderNo, String customerId, List<ErpOrderLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("履约明细不能为空");
        }
        // TODO（学习任务）：接收时锁定库存（F-502），库存不足拒绝接收
        return new ErpOrder(id, sourceOrderNo, customerId, new ArrayList<>(lines));
    }

    /** 发货：已接收 → 已发货 */
    public void ship() {
        transitTo(ErpOrderStatus.SHIPPED);
        this.shippedAt = LocalDateTime.now();
        // TODO（学习任务）：出库扣减库存
    }

    /** 签收：已发货 → 已签收（终态） */
    public void sign() {
        transitTo(ErpOrderStatus.SIGNED);
        this.signedAt = LocalDateTime.now();
        // TODO（学习任务）：释放库存占用
    }

    /** 关闭：已接收/已发货 → 已关闭（终态） */
    public void close() {
        transitTo(ErpOrderStatus.CLOSED);
    }

    /** 取消：已接收/已发货 → 已取消（终态） */
    public void cancel() {
        transitTo(ErpOrderStatus.CANCELLED);
        // TODO（学习任务）：释放库存占用
    }

    /**
     * 统一状态迁移入口（状态机 = 幂等核心）。
     */
    private void transitTo(ErpOrderStatus target) {
        if (!status.canTransitTo(target)) {
            throw new IllegalStateException(
                    "非法状态迁移: " + status + " -> " + target);
        }
        this.status = target;
        this.updateTime = LocalDateTime.now();
        // TODO（学习任务）：发布 ErpOrderStatusChangedEvent（由应用层统一发布）
    }

    // ---------- getters ----------

    public ErpOrderId getId() {
        return id;
    }

    public String getSourceOrderNo() {
        return sourceOrderNo;
    }

    public String getCustomerId() {
        return customerId;
    }

    public ErpOrderStatus getStatus() {
        return status;
    }

    public List<ErpOrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
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
