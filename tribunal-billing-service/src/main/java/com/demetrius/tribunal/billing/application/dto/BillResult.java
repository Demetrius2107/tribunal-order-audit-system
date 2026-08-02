package com.demetrius.tribunal.billing.application.dto;

import com.demetrius.tribunal.billing.domain.model.FinanceBill;
import com.demetrius.tribunal.billing.domain.model.BillLine;
import com.demetrius.tribunal.billing.domain.model.BillStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 金融账单应用层出参。
 */
public record BillResult(
        String billId,
        String sourceOrderNo,
        String customerId,
        BillStatus status,
        BigDecimal totalAmount,
        LocalDateTime generatedAt,
        LocalDateTime confirmedAt,
        LocalDateTime settledAt,
        List<BillLineResult> lines) {

    public record BillLineResult(
            String skuCode,
            String skuName,
            BigDecimal quantity,
            BigDecimal price,
            BigDecimal amount) {
    }

    /** 聚合 → 应用层 DTO（TODO：可抽成 Assembler） */
    public static BillResult from(FinanceBill bill) {
        List<BillLineResult> lineResults = bill.getLines().stream()
                .map(l -> new BillLineResult(l.getSkuCode(), l.getSkuName(),
                        l.getQuantity(), l.getPrice(), l.getAmount()))
                .toList();
        return new BillResult(
                bill.getId().value(),
                bill.getSourceOrderNo(),
                bill.getCustomerId(),
                bill.getStatus(),
                bill.getTotalAmount(),
                bill.getGeneratedAt(),
                bill.getConfirmedAt(),
                bill.getSettledAt(),
                lineResults);
    }
}
