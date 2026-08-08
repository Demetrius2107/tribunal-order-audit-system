package com.demetrius.tribunal.marketing.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 优惠券模板聚合根（★核心类）。
 *
 * <p>券模板定义券的「规则」，UserCoupon 是用户领取的「实例」。</p>
 *
 * <p>字段含义（按类型）：</p>
 * <table>
 *   <tr><th>类型</th><th>threshold</th><th>deductionAmount</th><th>discountRate</th></tr>
 *   <tr><td>FULL_REDUCTION</td><td>满 N 元</td><td>减 M 元</td><td>-</td></tr>
 *   <tr><td>DISCOUNT</td><td>-</td><td>-</td><td>0.9 = 九折</td></tr>
 * </table>
 *
 * <p>防刷规则：</p>
 * <ul>
 *   <li>{@link #totalQuota} — 券总发放量（null = 不限）</li>
 *   <li>{@link #perUserLimit} — 每人限领数量（默认 1）</li>
 *   <li>{@link #issuedCount} — 已发放数量（领券时自增，防超发）</li>
 * </ul>
 */
public class CouponTemplate {

    private final String id;
    private final String templateNo;
    private final String name;
    private final CouponType type;

    /** 满减门槛（FULL_REDUCTION 使用） */
    private final BigDecimal threshold;
    /** 满减金额（FULL_REDUCTION 使用） */
    private final BigDecimal deductionAmount;
    /** 折扣率（DISCOUNT 使用，0.9 = 九折） */
    private final BigDecimal discountRate;

    /** 总发放量（null = 不限） */
    private final Integer totalQuota;
    /** 每人限领数量（默认 1） */
    private final int perUserLimit;

    private final LocalDateTime validStartTime;
    private final LocalDateTime validEndTime;

    private boolean active;
    private int issuedCount;

    private final LocalDateTime createTime;
    private LocalDateTime updateTime;

    private CouponTemplate(String id, String templateNo, String name, CouponType type,
                           BigDecimal threshold, BigDecimal deductionAmount, BigDecimal discountRate,
                           Integer totalQuota, int perUserLimit,
                           LocalDateTime validStartTime, LocalDateTime validEndTime,
                           boolean active, int issuedCount,
                           LocalDateTime createTime, LocalDateTime updateTime) {
        this.id = id;
        this.templateNo = templateNo;
        this.name = name;
        this.type = type;
        this.threshold = threshold;
        this.deductionAmount = deductionAmount;
        this.discountRate = discountRate;
        this.totalQuota = totalQuota;
        this.perUserLimit = perUserLimit;
        this.validStartTime = validStartTime;
        this.validEndTime = validEndTime;
        this.active = active;
        this.issuedCount = issuedCount;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    /**
     * 工厂方法：创建券模板。
     */
    public static CouponTemplate create(String id, String templateNo, String name, CouponType type,
                                        BigDecimal threshold, BigDecimal deductionAmount, BigDecimal discountRate,
                                        Integer totalQuota, int perUserLimit,
                                        LocalDateTime validStartTime, LocalDateTime validEndTime) {
        Objects.requireNonNull(id, "券模板ID不能为空");
        Objects.requireNonNull(templateNo, "券模板编号不能为空");
        Objects.requireNonNull(name, "券模板名称不能为空");
        Objects.requireNonNull(type, "券类型不能为空");
        validateFields(type, threshold, deductionAmount, discountRate);
        if (validStartTime != null && validEndTime != null
                && validStartTime.isAfter(validEndTime)) {
            throw new IllegalArgumentException("有效期开始时间不能晚于结束时间");
        }
        if (perUserLimit <= 0) {
            throw new IllegalArgumentException("每人限领数量必须大于0");
        }
        LocalDateTime now = LocalDateTime.now();
        return new CouponTemplate(id, templateNo, name, type,
                threshold, deductionAmount, discountRate,
                totalQuota, perUserLimit,
                validStartTime, validEndTime,
                true, 0, now, now);
    }

    /**
     * 还原工厂方法：从持久化数据完整还原。
     */
    public static CouponTemplate restore(String id, String templateNo, String name, CouponType type,
                                         BigDecimal threshold, BigDecimal deductionAmount, BigDecimal discountRate,
                                         Integer totalQuota, int perUserLimit,
                                         LocalDateTime validStartTime, LocalDateTime validEndTime,
                                         boolean active, int issuedCount,
                                         LocalDateTime createTime, LocalDateTime updateTime) {
        return new CouponTemplate(id, templateNo, name, type,
                threshold, deductionAmount, discountRate,
                totalQuota, perUserLimit,
                validStartTime, validEndTime,
                active, issuedCount, createTime, updateTime);
    }

    private static void validateFields(CouponType type, BigDecimal threshold,
                                       BigDecimal deductionAmount, BigDecimal discountRate) {
        switch (type) {
            case FULL_REDUCTION -> {
                if (threshold == null || threshold.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("满减门槛不能为负");
                }
                if (deductionAmount == null || deductionAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("满减金额必须大于0");
                }
            }
            case DISCOUNT -> {
                if (discountRate == null
                        || discountRate.compareTo(BigDecimal.ZERO) <= 0
                        || discountRate.compareTo(BigDecimal.ONE) >= 0) {
                    throw new IllegalArgumentException("折扣率必须在 (0, 1) 之间");
                }
            }
        }
    }

    // ---------- 行为 ----------

    /** 判断券模板是否在有效期内 */
    public boolean isValid(LocalDateTime now) {
        if (!active) {
            return false;
        }
        if (validStartTime != null && now.isBefore(validStartTime)) {
            return false;
        }
        return validEndTime == null || !now.isAfter(validEndTime);
    }

    /** 是否已发完（totalQuota = null 表示不限） */
    public boolean isExhausted() {
        return totalQuota != null && issuedCount >= totalQuota;
    }

    /** 发放一张券（issuedCount++） */
    public void issue() {
        if (isExhausted()) {
            throw new IllegalStateException("券已发完: " + templateNo);
        }
        this.issuedCount++;
        this.updateTime = LocalDateTime.now();
    }

    /** 归还一张券（取消领券/退券时 issuedCount--） */
    public void returnBack() {
        if (this.issuedCount > 0) {
            this.issuedCount--;
            this.updateTime = LocalDateTime.now();
        }
    }

    /** 停用 */
    public void deactivate() {
        this.active = false;
        this.updateTime = LocalDateTime.now();
    }

    /** 激活 */
    public void activate() {
        this.active = true;
        this.updateTime = LocalDateTime.now();
    }

    // ---------- getters ----------

    public String getId() { return id; }
    public String getTemplateNo() { return templateNo; }
    public String getName() { return name; }
    public CouponType getType() { return type; }
    public BigDecimal getThreshold() { return threshold; }
    public BigDecimal getDeductionAmount() { return deductionAmount; }
    public BigDecimal getDiscountRate() { return discountRate; }
    public Integer getTotalQuota() { return totalQuota; }
    public int getPerUserLimit() { return perUserLimit; }
    public LocalDateTime getValidStartTime() { return validStartTime; }
    public LocalDateTime getValidEndTime() { return validEndTime; }
    public boolean isActive() { return active; }
    public int getIssuedCount() { return issuedCount; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
}
