package com.demetrius.tribunal.erp.domain.model;

/**
 * ERP 履约订单 ID 值对象。
 */
public record ErpOrderId(String value) {

    public ErpOrderId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("履约订单ID不能为空");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
