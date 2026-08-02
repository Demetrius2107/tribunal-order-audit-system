package com.demetrius.tribunal.order.client;

import java.math.BigDecimal;
import java.util.List;

/**
 * 转单请求体（order-service → erp-service）。
 *
 * <p>对应需求：F-307（转单）。</p>
 */
public record ErpTransferRequest(
        String sourceOrderNo,
        String customerId,
        List<ErpTransferLine> lines) {

    public record ErpTransferLine(
            String skuCode,
            String skuName,
            BigDecimal quantity,
            BigDecimal price) {
    }
}
