package com.demetrius.tribunal.common.event;

/**
 * 订单审单通过事件（order-service 发布 → finance-settlement-service 订阅建结算单）。
 *
 * <p>R1 契约占位；M3 异步化时补充金额/明细/账期等完整字段。</p>
 */
public record OrderApprovedIntegrationEvent(
        String eventId,
        long occurredAt,
        String orderId,
        String customerId
) implements IntegrationEvent {
}
