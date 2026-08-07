package com.demetrius.tribunal.common.event;

/**
 * 账单状态变更事件（finance-settlement-service 发布 → order-service 订阅回传状态）。
 *
 * <p>R1 契约占位；M3 异步化时补充金额/支付方式/变更原因等字段。</p>
 */
public record BillStatusChangedIntegrationEvent(
        String eventId,
        long occurredAt,
        String orderId,
        String billId,
        String billStatus
) implements IntegrationEvent {
}
