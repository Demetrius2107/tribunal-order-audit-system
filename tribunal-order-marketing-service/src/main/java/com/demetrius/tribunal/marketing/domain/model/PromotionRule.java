package com.demetrius.tribunal.marketing.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 促销规则聚合根（M4 促销引擎核心模型）。
 *
 * <p>支持四种促销玩法（{@link PromotionType}），可配置适用对象、有效期、优先级与互斥。</p>
 *
 * <p>叠加/互斥规则：</p>
 * <ul>
 *   <li>引擎按 {@link #priority} 升序逐条应用</li>
 *   <li>{@link #exclusive} = true 的规则应用后，后续规则全部跳过</li>
 *   <li>非互斥规则按顺序累扣（每条规则基于"剩余可折扣金额"计算）</li>
 * </ul>
 *
 * <p>不同类型使用的字段：</p>
 * <table>
 *   <tr><th>类型</th><th>threshold</th><th>discountRate</th><th>reductionAmount</th><th>halfPriceRate</th><th>gift*</th></tr>
 *   <tr><td>FULL_REDUCTION</td><td>满 N 元</td><td>-</td><td>减 M 元</td><td>-</td><td>-</td></tr>
 *   <tr><td>DISCOUNT</td><td>-</td><td>0.9 = 九折</td><td>-</td><td>-</td><td>-</td></tr>
 *   <tr><td>SECOND_HALF_PRICE</td><td>-</td><td>-</td><td>-</td><td>0.5</td><td>-</td></tr>
 *   <tr><td>GIFT</td><td>满 N 元</td><td>-</td><td>-</td><td>-</td><td>giftSkuCode/Name/Quantity</td></tr>
 * </table>
 */
public class PromotionRule {

    private final String id;
    private final String ruleNo;
    private final String name;
    private final PromotionType type;
    private final PromotionTargetType targetType;
    private final String targetValue;

    /** 满减/满赠门槛金额（FULL_REDUCTION、GIFT 使用） */
    private final BigDecimal threshold;
    /** 折扣率（DISCOUNT 使用，0.9 = 九折，即减免 10%） */
    private final BigDecimal discountRate;
    /** 满减金额（FULL_REDUCTION 使用） */
    private final BigDecimal reductionAmount;
    /** 第二件折扣率（SECOND_HALF_PRICE 使用，默认 0.5 = 半价） */
    private final BigDecimal halfPriceRate;
    /** 限定的 SKU 编码（null = 不限定，对整单所有 SKU 生效） */
    private final String applicableSkuCode;

    private final String giftSkuCode;
    private final String giftSkuName;
    private final BigDecimal giftQuantity;

    private final boolean exclusive;
    private final int priority;
    private final boolean active;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

    public PromotionRule(String id, String ruleNo, String name, PromotionType type,
                         PromotionTargetType targetType, String targetValue,
                         BigDecimal threshold, BigDecimal discountRate,
                         BigDecimal reductionAmount, BigDecimal halfPriceRate,
                         String applicableSkuCode,
                         String giftSkuCode, String giftSkuName, BigDecimal giftQuantity,
                         boolean exclusive, int priority, boolean active,
                         LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.ruleNo = ruleNo;
        this.name = name;
        this.type = type;
        this.targetType = targetType;
        this.targetValue = targetValue;
        this.threshold = threshold;
        this.discountRate = discountRate;
        this.reductionAmount = reductionAmount;
        this.halfPriceRate = halfPriceRate == null ? new BigDecimal("0.5") : halfPriceRate;
        this.applicableSkuCode = applicableSkuCode;
        this.giftSkuCode = giftSkuCode;
        this.giftSkuName = giftSkuName;
        this.giftQuantity = giftQuantity;
        this.exclusive = exclusive;
        this.priority = priority;
        this.active = active;
        this.startTime = startTime;
        this.endTime = endTime;
        validate();
    }

    private void validate() {
        if (type == null) {
            throw new IllegalArgumentException("促销类型不能为空");
        }
        if (targetType == null) {
            throw new IllegalArgumentException("适用对象类型不能为空");
        }
        if (targetType != PromotionTargetType.ALL
                && (targetValue == null || targetValue.isBlank())) {
            throw new IllegalArgumentException("非全量促销必须指定 targetValue");
        }
        // 类型相关字段校验
        switch (type) {
            case FULL_REDUCTION -> {
                requireNonNegative(threshold, "满减门槛");
                requireNonNegative(reductionAmount, "满减金额");
            }
            case DISCOUNT -> {
                if (discountRate == null
                        || discountRate.compareTo(BigDecimal.ZERO) <= 0
                        || discountRate.compareTo(BigDecimal.ONE) > 0) {
                    throw new IllegalArgumentException("折扣率必须在 (0, 1] 之间");
                }
            }
            case SECOND_HALF_PRICE -> {
                if (this.halfPriceRate.compareTo(BigDecimal.ZERO) < 0
                        || this.halfPriceRate.compareTo(BigDecimal.ONE) > 0) {
                    throw new IllegalArgumentException("第二件折扣率必须在 [0, 1] 之间");
                }
            }
            case GIFT -> {
                requireNonNegative(threshold, "满赠门槛");
                if (giftSkuCode == null || giftSkuCode.isBlank()) {
                    throw new IllegalArgumentException("满赠必须指定赠品 SKU");
                }
                if (giftQuantity == null || giftQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("赠品数量必须大于 0");
                }
            }
        }
    }

    private static void requireNonNegative(BigDecimal v, String label) {
        if (v == null || v.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(label + "不能为负");
        }
    }

    /** 是否在有效期内且启用 */
    public boolean isEffective(LocalDateTime now) {
        if (!active) {
            return false;
        }
        if (startTime != null && now.isBefore(startTime)) {
            return false;
        }
        return endTime == null || !now.isAfter(endTime);
    }

    /** 是否匹配促销上下文（客户/客户组） */
    public boolean matches(PromotionContext ctx) {
        return switch (targetType) {
            case ALL -> true;
            case CUSTOMER -> ctx != null && targetValue != null && targetValue.equals(ctx.customerCode());
            case CUSTOMER_GROUP -> ctx != null && targetValue != null && targetValue.equals(ctx.customerGroupId());
        };
    }

    /** 是否限定到指定 SKU（用于第二件半价等 SKU 级规则） */
    public boolean appliesToSku(String skuCode) {
        return applicableSkuCode == null || applicableSkuCode.isBlank()
                || applicableSkuCode.equals(skuCode);
    }

    // ===== getters =====

    public String getId() { return id; }
    public String getRuleNo() { return ruleNo; }
    public String getName() { return name; }
    public PromotionType getType() { return type; }
    public PromotionTargetType getTargetType() { return targetType; }
    public String getTargetValue() { return targetValue; }
    public BigDecimal getThreshold() { return threshold; }
    public BigDecimal getDiscountRate() { return discountRate; }
    public BigDecimal getReductionAmount() { return reductionAmount; }
    public BigDecimal getHalfPriceRate() { return halfPriceRate; }
    public String getApplicableSkuCode() { return applicableSkuCode; }
    public String getGiftSkuCode() { return giftSkuCode; }
    public String getGiftSkuName() { return giftSkuName; }
    public BigDecimal getGiftQuantity() { return giftQuantity; }
    public boolean isExclusive() { return exclusive; }
    public int getPriority() { return priority; }
    public boolean isActive() { return active; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
}
