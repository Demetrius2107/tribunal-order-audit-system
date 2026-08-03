package com.demetrius.tribunal.inventory.application.dto;

import com.demetrius.tribunal.inventory.domain.model.InventoryItem;

import java.math.BigDecimal;

/**
 * 库存物料应用层出参。
 */
public record InventoryItemResult(
        String id,
        String skuCode,
        String skuName,
        String unit,
        BigDecimal totalQuantity,
        BigDecimal reservedQuantity,
        BigDecimal availableQuantity) {

    /** 聚合 → 应用层 DTO */
    public static InventoryItemResult from(InventoryItem item) {
        return new InventoryItemResult(
                item.getId().value(),
                item.getSkuCode(),
                item.getSkuName(),
                item.getUnit(),
                item.getTotalQuantity(),
                item.getReservedQuantity(),
                item.availableQuantity());
    }
}
