package com.demetrius.tribunal.erp.domain.model;

import java.math.BigDecimal;

/**
 * ERP 履约订单明细（聚合内实体）。
 *
 * <p>对应需求：F-307（转单明细）、F-501（库存）。</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>补充发货数量（shippedQuantity）、签收数量（signedQuantity），支持部分发货/签收</li>
 *   <li>补充批次/库位（库存管理细化）</li>
 *   <li>金额计算与 OMS 明细核对（对账依据）</li>
 * </ul>
 */
public class ErpOrderLine {

    private final String skuCode;

    private final String skuName;

    private final BigDecimal quantity;

    private final BigDecimal price;

    private BigDecimal amount;

    public ErpOrderLine(String skuCode, String skuName, BigDecimal quantity, BigDecimal price) {
        if (skuCode == null || skuCode.isBlank()) {
            throw new IllegalArgumentException("SKU编码不能为空");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("SKU数量必须大于0");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("SKU单价不能为负");
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
