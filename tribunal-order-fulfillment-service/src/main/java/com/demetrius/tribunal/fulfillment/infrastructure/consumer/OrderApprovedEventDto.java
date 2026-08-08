package com.demetrius.tribunal.fulfillment.infrastructure.consumer;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单审单通过事件 DTO（order-service → fulfillment-service，Kafka 契约）。
 *
 * <p>字段名与 order-service 的 OrderEventMessage JSON 一致（跨系统契约）。</p>
 */
public record OrderApprovedEventDto(
        String eventId,
        String eventType,
        String orderId,
        String customerId,
        String merchantId,
        List<Item> items,
        BigDecimal shippingFee,
        BigDecimal discountAmount,
        String paymentMethod,
        String paymentCurrency,
        String orderTime,
        String shippingTime,
        String completeTime) {

    public record Item(
            String skuId,
            String skuName,
            int quantity,
            BigDecimal unitPrice,
            String category,
            String warehouseId) {
    }
}
