package com.demetrius.tribunal.erp.client;

/**
 * 状态回传请求体（erp-service → order-service）。
 */
public record OrderStatusCallbackRequest(
        String sourceOrderNo,
        String erpStatus,
        String erpOrderId) {
}
