package com.demetrius.tribunal.order.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 折扣上限策略（F-206：折扣上限校验）。
 *
 * <p>规则：单笔订单总折扣（促销+手动）不得超过商品总额的一定比例，
 * 防止促销叠加导致折扣失控、订单负毛利。</p>
 */
public class DiscountCapPolicy {

    /** 折扣上限比例：默认不超过商品总额的 50% */
    public static final BigDecimal MAX_DISCOUNT_RATE = new BigDecimal("0.50");

    /**
     * 计算折扣上限金额 = 商品总额 × 上限比例（四舍五入保留 2 位）。
     */
    public BigDecimal cap(BigDecimal totalAmount, BigDecimal discount) {
        BigDecimal base = totalAmount == null ? BigDecimal.ZERO : totalAmount;
        BigDecimal amount = discount == null ? BigDecimal.ZERO : discount;
        BigDecimal max = base.multiply(MAX_DISCOUNT_RATE).setScale(2, RoundingMode.HALF_UP);
        return amount.min(max);
    }

    /**
     * 校验折扣是否超限（超出上限抛异常，供下单/审单前置校验）。
     */
    public void validate(BigDecimal totalAmount, BigDecimal discount) {
        BigDecimal amount = discount == null ? BigDecimal.ZERO : discount;
        if (amount.compareTo(cap(totalAmount, amount)) > 0) {
            throw new IllegalArgumentException(
                    "折扣金额超过上限: " + amount + " > 商品总额×" + MAX_DISCOUNT_RATE
                            + "（" + cap(totalAmount, amount) + "）");
        }
    }
}
