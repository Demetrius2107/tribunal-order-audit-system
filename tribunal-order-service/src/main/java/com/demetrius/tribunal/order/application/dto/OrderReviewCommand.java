package com.demetrius.tribunal.order.application.dto;

/**
 * 审单应用层入参。
 *
 * <p>参照通用做法的入参。</p>
 */
public record OrderReviewCommand(
        String orderId,
        boolean approved,
        String reason,
        String operator) {
}
