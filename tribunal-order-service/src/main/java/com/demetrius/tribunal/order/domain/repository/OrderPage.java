package com.demetrius.tribunal.order.domain.repository;

import com.demetrius.tribunal.order.domain.model.Order;

import java.util.List;

/**
 * 订单分页查询结果（领域层返回值，避免持久化分页对象泄漏到领域层）。
 */
public record OrderPage(
        long total,
        long pageNum,
        long pageSize,
        List<Order> orders) {

    public static OrderPage of(long total, long pageNum, long pageSize, List<Order> orders) {
        return new OrderPage(total, pageNum, pageSize, orders);
    }
}
