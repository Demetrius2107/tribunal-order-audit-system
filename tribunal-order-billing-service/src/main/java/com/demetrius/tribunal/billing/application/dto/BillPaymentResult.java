package com.demetrius.tribunal.billing.application.dto;

import com.demetrius.tribunal.billing.infrastructure.model.BillPaymentPo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收款流水项（对外报表：账单收款明细查询出参）。
 */
public record BillPaymentResult(
        String id,
        String billId,
        String sourceOrderNo,
        BigDecimal amount,
        LocalDateTime paymentTime,
        String operator) {

    public static BillPaymentResult from(BillPaymentPo po) {
        return new BillPaymentResult(
                po.getId(),
                po.getBillId(),
                po.getSourceOrderNo(),
                po.getAmount(),
                po.getPaymentTime(),
                po.getOperator());
    }
}
