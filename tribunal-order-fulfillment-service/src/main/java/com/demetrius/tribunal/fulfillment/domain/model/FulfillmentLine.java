package com.demetrius.tribunal.fulfillment.domain.model;

import java.math.BigDecimal;

/**
 * 履约单明细（聚合内实体）。
 */
public class FulfillmentLine {

    private final String skuCode;

    private final String skuName;

    private final BigDecimal quantity;

    private final BigDecimal price;

    private BigDecimal amount;

    public FulfillmentLine(String skuCode, String skuName, BigDecimal quantity, BigDecimal price) {
        if (skuCode == null || skuCode.isBlank()) {
            throw new IllegalArgumentException("SKU编码不能为空");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("SKU数量必须大于0");
        }
        this.skuCode = skuCode;
        this.skuName = skuName;
        this.quantity = quantity;
        this.price = price;
        this.amount = quantity.multiply(price);
    }

    public String getSkuCode() {
        return skuCode;
    }

    public String getSkuName() {
        return skuName;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
