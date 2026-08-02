package com.demetrius.tribunal.fulfillment.domain.model;

/**
 * 履约单 ID 值对象。
 */
public record FulfillmentId(String value) {

    public FulfillmentId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("履约单ID不能为空");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
