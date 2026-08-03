package com.demetrius.tribunal.order.application.dto;

import java.util.List;

/**
 * 订单分页查询出参（应用层 DTO）。
 */
public record OrderPageResult(
        long total,
        long pageNum,
        long pageSize,
        List<OrderResult> orders) {

    public static OrderPageResult of(long total, long pageNum, long pageSize, List<OrderResult> orders) {
        return new OrderPageResult(total, pageNum, pageSize, orders);
    }
}
