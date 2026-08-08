package com.demetrius.tribunal.marketing.domain.model;

/**
 * 优惠券类型。
 *
 * <ul>
 *   <li>{@link #FULL_REDUCTION} — 满减券：满 N 元减 M 元（threshold + deductionAmount）</li>
 *   <li>{@link #DISCOUNT} — 折扣券：按折扣率计算（discountRate，0.9 = 九折）</li>
 * </ul>
 */
public enum CouponType {

    /** 满减券 */
    FULL_REDUCTION,

    /** 折扣券 */
    DISCOUNT
}
