package com.demetrius.tribunal.order.application.dto;

import java.math.BigDecimal;

/**
 * 账单状态事件 DTO（billing-service → order-service，Kafka 契约）。
 *
 * <p>字段名与 billing-service 发布的 JSON 一致（跨系统契约）。</p>
 */
public record BillingEventDto(
        String eventId,
        String orderId,
        String billId,
        String billStatus,
        BigDecimal amount,
        String paymentMethod,
        long timestamp
) {
}
