package com.demetrius.tribunal.order.application.dto;

/**
 * 审单应用层入参。
 *
 * <p>对照旧项目：{@code SalesmanController.reviewOrder}（审核订单接口）的入参。</p>
 */
public record OrderReviewCommand(
        String orderId,
        boolean approved,
        String reason,
        String operator) {
}
