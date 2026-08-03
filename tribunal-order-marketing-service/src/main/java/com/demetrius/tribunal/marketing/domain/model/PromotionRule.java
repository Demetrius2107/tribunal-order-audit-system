package com.demetrius.tribunal.marketing.domain.model;

import java.math.BigDecimal;

/**
 * 促销规则聚合根。
 *
 * <p>对应需求：F-201/F-202（客户型/客户组型促销，关联 SKU 组，按行计算折扣）。</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>促销类型扩展：满减/满赠/折扣率</li>
 *   <li>促销有效期与活动状态</li>
 *   <li>促销与押金/税的叠加顺序</li>
 * </ul>
 */
public class PromotionRule {

    private final String id;

    /** 促销类型：CUSTOMER（客户）/ CUSTOMER_GROUP（客户组） */
    private final String promotionType;

    /** 促销对象编码（客户编码/客户组编码） */
    private final String promotionTarget;

    /** 折扣率（0~1，如 0.1 表示 9 折） */
    private final BigDecimal discountRate;

    private final boolean active;

    public PromotionRule(String id, String promotionType, String promotionTarget,
                         BigDecimal discountRate, boolean active) {
        if (discountRate == null || discountRate.compareTo(BigDecimal.ZERO) < 0
                || discountRate.compareTo(BigDecimal.ONE) > 1) {
            throw new IllegalArgumentException("折扣率必须在0~1之间");
        }
        this.id = id;
        this.promotionType = promotionType;
        this.promotionTarget = promotionTarget;
        this.discountRate = discountRate;
        this.active = active;
    }

    /** 计算折扣后金额：amount * (1 - discountRate) */
    public BigDecimal applyTo(BigDecimal amount) {
        return amount.multiply(BigDecimal.ONE.subtract(discountRate));
    }

    public String getId() {
        return id;
    }

    public String getPromotionType() {
        return promotionType;
    }

    public String getPromotionTarget() {
        return promotionTarget;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public boolean isActive() {
        return active;
    }
}
