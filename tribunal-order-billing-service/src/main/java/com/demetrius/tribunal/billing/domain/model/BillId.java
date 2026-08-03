package com.demetrius.tribunal.billing.domain.model;

/**
 * 金融账单订单 ID 值对象。
 */
public record BillId(String value) {

    public BillId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("履约订单ID不能为空");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
