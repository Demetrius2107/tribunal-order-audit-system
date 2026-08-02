package com.demetrius.tribunal.fulfillment.application.dto;

import com.demetrius.tribunal.fulfillment.domain.model.FulfillmentLine;
import com.demetrius.tribunal.fulfillment.domain.model.FulfillmentOrder;
import com.demetrius.tribunal.fulfillment.domain.model.FulfillmentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 履约单应用层出参。
 */
public record FulfillmentResult(
        String fulfillmentId,
        String sourceOrderNo,
        String customerId,
        FulfillmentStatus status,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        LocalDateTime shippedAt,
        LocalDateTime signedAt,
        List<FulfillmentLineResult> lines) {

    public record FulfillmentLineResult(
            String skuCode,
            String skuName,
            BigDecimal quantity,
            BigDecimal price,
            BigDecimal amount) {
    }

    public static FulfillmentResult from(FulfillmentOrder order) {
        List<FulfillmentLineResult> lineResults = order.getLines().stream()
                .map(l -> new FulfillmentLineResult(l.getSkuCode(), l.getSkuName(),
                        l.getQuantity(), l.getPrice(), l.getAmount()))
                .toList();
        return new FulfillmentResult(
                order.getId().value(),
                order.getSourceOrderNo(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getShippedAt(),
                order.getSignedAt(),
                lineResults);
    }
}
