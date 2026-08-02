package com.demetrius.tribunal.billing.client;

/**
 * 状态回传请求体（billing-service → order-service）。
 */
public record OrderStatusCallbackRequest(
        String sourceOrderNo,
        String billStatus,
        String billId) {
}
