package com.demetrius.tribunal.billing.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 金融账单聚合根。
 *
 * <p>对应需求：F-307（生成账单）、F-404（收款确认）、N-304（状态回传幂等）。</p>
 *
 * <p>职责：账单侧对订单的财务生命周期——生成、确认、结算、核销、取消。
 * 业务规则（状态机校验、金额核对）内聚在聚合内部。</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>金额核对：账单金额与订单金额一致性校验（对账依据，对应 F-701）</li>
 *   <li>收款流水：结算时记录收款明细（对应 F-404 收款确认）</li>
 *   <li>发票/凭证：发票开具与凭证关联（财务细化）</li>
 * </ul>
 */
public class FinanceBill {

    private final BillId id;

    /** 上游订单服务订单编号（业务关联键，回传状态时携带） */
    private final String sourceOrderNo;

    private final String customerId;

    private BillStatus status;

    private final List<BillLine> lines;

    private BigDecimal totalAmount;

    private LocalDateTime generatedAt;

    private LocalDateTime confirmedAt;

    private LocalDateTime settledAt;

    private LocalDateTime updateTime;

    private FinanceBill(BillId id, String sourceOrderNo, String customerId, List<BillLine> lines) {
        this.id = id;
        this.sourceOrderNo = sourceOrderNo;
        this.customerId = customerId;
        this.lines = lines;
        this.status = BillStatus.GENERATED;
        this.generatedAt = LocalDateTime.now();
        this.updateTime = this.generatedAt;
        this.totalAmount = lines.stream()
                .map(BillLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 工厂方法：订单服务转单到达，生成账单（初始状态 = 已生成）。
     */
    public static FinanceBill generate(BillId id, String sourceOrderNo, String customerId, List<BillLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("账单明细不能为空");
        }
        return new FinanceBill(id, sourceOrderNo, customerId, new ArrayList<>(lines));
    }

    /** 确认：已生成 → 已确认（账单审核通过，待收款） */
    public void confirm() {
        transitTo(BillStatus.CONFIRMED);
        this.confirmedAt = LocalDateTime.now();
    }

    /** 结算：已确认 → 已结算（款项到位，终态） */
    public void settle() {
        transitTo(BillStatus.SETTLED);
        this.settledAt = LocalDateTime.now();
        // TODO（学习任务）：记录收款流水（F-404）
    }

    /** 核销：已确认 → 已核销（账务核销完成，终态） */
    public void verify() {
        transitTo(BillStatus.VERIFIED);
        this.settledAt = LocalDateTime.now();
    }

    /** 取消：已生成/已确认 → 已取消（终态） */
    public void cancel() {
        transitTo(BillStatus.CANCELLED);
    }

    /**
     * 统一状态迁移入口（状态机 = 幂等核心）。
     */
    private void transitTo(BillStatus target) {
        if (!status.canTransitTo(target)) {
            throw new IllegalStateException(
                    "非法状态迁移: " + status + " -> " + target);
        }
        this.status = target;
        this.updateTime = LocalDateTime.now();
        // TODO（学习任务）：发布 BillStatusChangedEvent（由应用层统一发布）
    }

    // ---------- getters ----------

    public BillId getId() {
        return id;
    }

    public String getSourceOrderNo() {
        return sourceOrderNo;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BillStatus getStatus() {
        return status;
    }

    public List<BillLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public LocalDateTime getSettledAt() {
        return settledAt;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
}
