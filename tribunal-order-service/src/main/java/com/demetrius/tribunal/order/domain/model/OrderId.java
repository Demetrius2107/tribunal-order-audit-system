package com.demetrius.tribunal.order.domain.model;

/**
 * 订单 ID 值对象。
 *
 * <p>用值对象而非 String，是为了让类型系统区分「订单 ID」与「普通字符串」，
 * 避免把 customerId 误传给订单 ID 位置（对照旧项目大量 String id 导致的问题）。</p>
 */
public record OrderId(String value) {

    public OrderId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("订单ID不能为空");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
