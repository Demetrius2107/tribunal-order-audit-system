package com.demetrius.tribunal.inventory.domain.model;

/**
 * 库存物料 ID 值对象。
 */
public record InventoryItemId(String value) {

    public InventoryItemId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("物料ID不能为空");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
