package com.demetrius.tribunal.erp.application.dto;

import com.demetrius.tribunal.erp.domain.model.ErpOrder;
import com.demetrius.tribunal.erp.domain.model.ErpOrderLine;
import com.demetrius.tribunal.erp.domain.model.ErpOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ERP 履约订单应用层出参。
 */
public record ErpOrderResult(
        String erpOrderId,
        String sourceOrderNo,
        String customerId,
        ErpOrderStatus status,
        BigDecimal totalAmount,
        LocalDateTime receivedAt,
        LocalDateTime shippedAt,
        LocalDateTime signedAt,
        List<ErpOrderLineResult> lines) {

    public record ErpOrderLineResult(
            String skuCode,
            String skuName,
            BigDecimal quantity,
            BigDecimal price,
            BigDecimal amount) {
    }

    /** 聚合 → 应用层 DTO（TODO：可抽成 Assembler） */
    public static ErpOrderResult from(ErpOrder order) {
        List<ErpOrderLineResult> lineResults = order.getLines().stream()
                .map(l -> new ErpOrderLineResult(l.getSkuCode(), l.getSkuName(),
                        l.getQuantity(), l.getPrice(), l.getAmount()))
                .toList();
        return new ErpOrderResult(
                order.getId().value(),
                order.getSourceOrderNo(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getReceivedAt(),
                order.getShippedAt(),
                order.getSignedAt(),
                lineResults);
    }
}
