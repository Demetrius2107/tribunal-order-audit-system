package com.demetrius.tribunal.order.application.dto;

import com.demetrius.tribunal.order.domain.model.Order;
import com.demetrius.tribunal.order.domain.model.OrderSku;
import com.demetrius.tribunal.order.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单应用层出参。
 *
 * <p>由领域聚合 {@link Order} 转换而来（应用服务做转换，Controller 不做领域逻辑）。</p>
 */
public record OrderResult(
        String orderId,
        String orderNo,
        String customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal payableAmount,
        String rejectReason,
        LocalDateTime createTime,
        List<SkuResult> skus) {

    public record SkuResult(
            String skuCode,
            String skuName,
            BigDecimal quantity,
            BigDecimal price,
            BigDecimal amount) {
    }

    /** 聚合 → 应用层 DTO（TODO：可抽成 Assembler，本骨架先放这里） */
    public static OrderResult from(Order order) {
        List<SkuResult> skuResults = order.getSkus().stream()
                .map(s -> new SkuResult(s.getSkuCode(), s.getSkuName(),
                        s.getQuantity(), s.getPrice(), s.getAmount()))
                .toList();
        return new OrderResult(
                order.getId().value(),
                order.getOrderNo(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getDiscountAmount(),
                order.getPayableAmount(),
                order.getRejectReason(),
                order.getCreateTime(),
                skuResults);
    }
}
