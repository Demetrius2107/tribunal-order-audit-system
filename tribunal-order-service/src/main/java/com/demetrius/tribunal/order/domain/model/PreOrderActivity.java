package com.demetrius.tribunal.order.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 预购活动聚合根（F-312：提前采购，经销商预付/保证金模式）。
 *
 * <p>预购活动 = 参与 SKU 范围 + 保证金比例 + 预购专享折扣率 + 有效期。
 * 预购单走独立计价口径（业务文档七节：totalCalculatePreOrderDiscount /
 * calculateDiscountPoolAmountPreOrder 的简化版）。</p>
 *
 * <p>业务规则：</p>
 * <ul>
 *   <li>仅进行中（ACTIVE）且有效期内可参与</li>
 *   <li>SKU 必须在活动范围内</li>
 *   <li>预购计价：预购价 = 单价 × 预购专享折扣率（独立于普通促销）</li>
 *   <li>保证金 = 预购应付总额 × 保证金比例；补缴 = 总额 - 保证金</li>
 * </ul>
 */
public class PreOrderActivity {

    private final String id;

    /** 预购活动编号（业务唯一键） */
    private final String activityNo;

    private final String name;

    /** 参与 SKU 范围（可为空 = 全部 SKU） */
    private final List<String> skuCodes;

    /** 保证金比例（0~1，如 0.3 = 30%） */
    private final BigDecimal depositRate;

    /** 预购专享折扣率（0~1，如 0.9 = 9 折） */
    private final BigDecimal discountRate;

    private final LocalDateTime startTime;

    private final LocalDateTime endTime;

    private PreOrderActivityStatus status;

    private final LocalDateTime createTime;

    private LocalDateTime updateTime;

    private PreOrderActivity(String id, String activityNo, String name, List<String> skuCodes,
                             BigDecimal depositRate, BigDecimal discountRate,
                             LocalDateTime startTime, LocalDateTime endTime,
                             PreOrderActivityStatus status,
                             LocalDateTime createTime, LocalDateTime updateTime) {
        if (activityNo == null || activityNo.isBlank()) {
            throw new IllegalArgumentException("预购活动编号不能为空");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("活动名称不能为空");
        }
        if (depositRate == null || depositRate.compareTo(BigDecimal.ZERO) < 0
                || depositRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("保证金比例必须在 0~1 之间");
        }
        if (discountRate == null || discountRate.compareTo(BigDecimal.ZERO) <= 0
                || discountRate.compareTo(BigDecimal.ONE) > 1) {
            throw new IllegalArgumentException("预购折扣率必须在 (0,1] 之间");
        }
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("活动结束时间必须晚于开始时间");
        }
        this.id = id;
        this.activityNo = activityNo;
        this.name = name;
        this.skuCodes = skuCodes == null ? new ArrayList<>() : new ArrayList<>(skuCodes);
        this.depositRate = depositRate;
        this.discountRate = discountRate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    /** 工厂：创建预购活动（初始状态 = 草稿）。 */
    public static PreOrderActivity create(String id, String activityNo, String name,
                                          List<String> skuCodes, BigDecimal depositRate,
                                          BigDecimal discountRate,
                                          LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        return new PreOrderActivity(id, activityNo, name, skuCodes, depositRate, discountRate,
                startTime, endTime, PreOrderActivityStatus.DRAFT, now, now);
    }

    /** 还原工厂：从持久化数据完整还原聚合（仓储读取时使用）。 */
    public static PreOrderActivity restore(String id, String activityNo, String name,
                                           List<String> skuCodes, BigDecimal depositRate,
                                           BigDecimal discountRate,
                                           LocalDateTime startTime, LocalDateTime endTime,
                                           PreOrderActivityStatus status,
                                           LocalDateTime createTime, LocalDateTime updateTime) {
        return new PreOrderActivity(id, activityNo, name, skuCodes, depositRate, discountRate,
                startTime, endTime, status, createTime, updateTime);
    }

    /** 上线：草稿 → 进行中。 */
    public void activate() {
        transitTo(PreOrderActivityStatus.ACTIVE);
    }

    /** 结束：进行中 → 已结束（终态）。 */
    public void end() {
        transitTo(PreOrderActivityStatus.ENDED);
    }

    /** 取消：草稿/进行中 → 已取消（终态）。 */
    public void cancel() {
        transitTo(PreOrderActivityStatus.CANCELLED);
    }

    /** 统一状态迁移入口（状态机 = 幂等核心）。 */
    private void transitTo(PreOrderActivityStatus target) {
        if (!status.canTransitTo(target)) {
            throw new IllegalStateException("非法状态迁移: " + status + " -> " + target);
        }
        this.status = target;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 校验活动当前可参与（进行中 + 有效期内 + SKU 在范围）。
     */
    public void validateParticipate(String skuCode, LocalDateTime now) {
        if (status != PreOrderActivityStatus.ACTIVE) {
            throw new IllegalStateException("预购活动未在进行中: " + activityNo + "（" + status + "）");
        }
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            throw new IllegalStateException("预购活动不在有效期内: " + activityNo);
        }
        if (!skuCodes.isEmpty() && !skuCodes.contains(skuCode)) {
            throw new IllegalStateException("SKU 不在预购活动范围内: " + skuCode);
        }
    }

    /**
     * 预购单价（独立计价口径）= 原单价 × 预购专享折扣率（四舍五入 2 位）。
     */
    public BigDecimal preOrderPrice(BigDecimal originalPrice) {
        return originalPrice.multiply(discountRate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 保证金金额 = 金额 × 保证金比例。
     */
    public BigDecimal depositAmount(BigDecimal amount) {
        return amount.multiply(depositRate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 补缴金额 = 金额 - 保证金。
     */
    public BigDecimal supplementAmount(BigDecimal amount) {
        return amount.subtract(depositAmount(amount)).setScale(2, RoundingMode.HALF_UP);
    }

    // ---------- getters ----------

    public String getId() {
        return id;
    }

    public String getActivityNo() {
        return activityNo;
    }

    public String getName() {
        return name;
    }

    public List<String> getSkuCodes() {
        return Collections.unmodifiableList(skuCodes);
    }

    public BigDecimal getDepositRate() {
        return depositRate;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public PreOrderActivityStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
}
