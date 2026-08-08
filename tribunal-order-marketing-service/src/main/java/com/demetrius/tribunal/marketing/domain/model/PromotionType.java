package com.demetrius.tribunal.marketing.domain.model;

/**
 * 促销类型（M4 促销引擎）。
 *
 * <p>四种主流促销玩法，覆盖啤酒经销场景：</p>
 * <ul>
 *   <li>{@link #FULL_REDUCTION} 满减——满 threshold 元减 reductionAmount 元</li>
 *   <li>{@link #DISCOUNT} 折扣——整单打 discountRate 折（如 0.9 = 九折）</li>
 *   <li>{@link #SECOND_HALF_PRICE} 第二件半价——每满 2 件第 2 件按 halfPriceRate 计价</li>
 *   <li>{@link #GIFT} 满赠——满 threshold 元赠 giftQuantity 件 giftSkuCode</li>
 * </ul>
 */
public enum PromotionType {
    FULL_REDUCTION,
    DISCOUNT,
    SECOND_HALF_PRICE,
    GIFT
}
