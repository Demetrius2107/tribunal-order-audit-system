package com.demetrius.tribunal.order.client;

/**
 * 转单响应体（erp-service → order-service）。
 */
public record ErpTransferResult(
        String erpOrderId,
        String sourceOrderNo,
        String status) {
}
