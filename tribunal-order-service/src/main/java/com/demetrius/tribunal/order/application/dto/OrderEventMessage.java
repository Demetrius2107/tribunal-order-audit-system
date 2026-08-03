package com.demetrius.tribunal.order.application.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单事件消息载荷（对应 PRD 4.1 Topic: order-events 事件体）。
 *
 * <p>订单系统发布，金融结算系统（Consumer Group: finance-settlement）订阅。
 * 采用 JSON 字符串传输，两侧各自维护 DTO，字段名作为契约。</p>
 */
public record OrderEventMessage(
        String eventId,
        String eventType,
        String orderId,
        String userId,
        String merchantId,
        List<Item> items,
        BigDecimal shippingFee,
        BigDecimal discountAmount,
        String paymentMethod,
        String paymentCurrency,
        String orderTime,
        String shippingTime,
        String completeTime) {

    /**
     * 订单明细项。
     */
    public record Item(
            String skuId,
            String skuName,
            Integer quantity,
            BigDecimal unitPrice,
            String category,
            String warehouseId) {
    }
}
