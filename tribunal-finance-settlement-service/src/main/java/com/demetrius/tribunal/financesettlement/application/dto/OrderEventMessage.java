package com.demetrius.tribunal.financesettlement.application.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单事件消息（对应 PRD 4.1 Topic: order-events 事件体）。
 *
 * <p>金融结算系统独立维护该 DTO（不复用订单系统的 common），字段名与订单系统发布端对齐作为契约。</p>
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

    /** 订单商品总金额 = Σ(SKU × 单价 × 数量) */
    public BigDecimal goodsAmount() {
        if (items == null) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 实付金额 = 商品金额 - 优惠 + 运费（PRD 2.1.2 FR-004/FR-005） */
    public BigDecimal netAmount() {
        BigDecimal discount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        BigDecimal shipping = shippingFee == null ? BigDecimal.ZERO : shippingFee;
        return goodsAmount().subtract(discount).add(shipping);
    }
}
