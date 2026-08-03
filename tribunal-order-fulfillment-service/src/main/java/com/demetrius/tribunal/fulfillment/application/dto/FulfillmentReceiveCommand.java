package com.demetrius.tribunal.fulfillment.application.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 履约接收入参（订单/账单结算后传入）。
 */
public record FulfillmentReceiveCommand(
        String sourceOrderNo,
        String customerId,
        List<FulfillmentLineItem> lines) {

    public record FulfillmentLineItem(
            String skuCode,
            String skuName,
            BigDecimal quantity,
            BigDecimal price) {
    }
}
