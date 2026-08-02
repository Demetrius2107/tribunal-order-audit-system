package com.demetrius.tribunal.order.client;

/**
 * 履约单结果（fulfillment-service → order-service）。
 */
public record FulfillmentResult(
        String fulfillmentId,
        String sourceOrderNo,
        String status) {
}
