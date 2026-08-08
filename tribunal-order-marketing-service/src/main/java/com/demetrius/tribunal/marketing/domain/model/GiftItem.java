package com.demetrius.tribunal.marketing.domain.model;

import java.math.BigDecimal;

/**
 * 赠品明细（满赠促销产出）。
 *
 * @param skuCode  赠品 SKU 编码
 * @param skuName  赠品名称
 * @param quantity 赠送数量
 */
public record GiftItem(String skuCode, String skuName, BigDecimal quantity) {
}
