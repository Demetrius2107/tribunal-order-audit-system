package com.demetrius.tribunal.marketing.domain.model;

import java.math.BigDecimal;

/**
 * 促销/押金计算的 SKU 输入项（与订单上下文解耦的不可变快照）。
 *
 * <p>营销域不依赖订单域的 {@code OrderSku}，由应用层在调用前做转换，
 * 保持两个限界上下文的独立性。</p>
 *
 * @param skuCode  SKU 编码
 * @param skuName  SKU 名称
 * @param quantity 数量
 * @param price    单价
 */
public record SkuItem(String skuCode, String skuName, BigDecimal quantity, BigDecimal price) {

    /** 行金额 = 数量 × 单价 */
    public BigDecimal amount() {
        return quantity.multiply(price);
    }
}
