package com.demetrius.tribunal.order.client;

/**
 * 转单响应体（billing-service → order-service）。
 */
public record BillTransferResult(
        String billId,
        String sourceOrderNo,
        String status) {
}
