package com.demetrius.tribunal.order.client;

import java.math.BigDecimal;
import java.util.List;

/**
 * 转单请求体（order-service → billing-service）。
 *
 * <p>对应需求：F-307（转单生成账单）。</p>
 */
public record BillTransferRequest(
        String sourceOrderNo,
        String customerId,
        List<BillTransferLine> lines) {

    public record BillTransferLine(
            String skuCode,
            String skuName,
            BigDecimal quantity,
            BigDecimal price) {
    }
}
