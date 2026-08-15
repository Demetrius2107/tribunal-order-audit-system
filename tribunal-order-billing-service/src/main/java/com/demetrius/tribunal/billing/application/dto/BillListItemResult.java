package com.demetrius.tribunal.billing.application.dto;

import com.demetrius.tribunal.billing.infrastructure.model.BillPo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账单列表项（对外报表：账单分页查询出参）。
 */
public record BillListItemResult(
        String billId,
        String sourceOrderNo,
        String customerId,
        String status,
        BigDecimal totalAmount,
        LocalDateTime generatedAt,
        LocalDateTime settledAt) {

    public static BillListItemResult from(BillPo po) {
        return new BillListItemResult(
                po.getId(),
                po.getSourceOrderNo(),
                po.getCustomerId(),
                po.getStatus(),
                po.getTotalAmount(),
                po.getGeneratedAt(),
                po.getSettledAt());
    }
}
