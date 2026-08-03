package com.demetrius.tribunal.order.client;

import java.math.BigDecimal;

/**
 * 库存物料查询结果（inventory-service → order-service）。
 */
public record InventoryItemResult(
        String id,
        String skuCode,
        String skuName,
        String unit,
        BigDecimal totalQuantity,
        BigDecimal reservedQuantity,
        BigDecimal availableQuantity) {
}
