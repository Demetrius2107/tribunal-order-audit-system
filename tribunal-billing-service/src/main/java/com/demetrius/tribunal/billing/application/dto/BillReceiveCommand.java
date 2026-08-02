package com.demetrius.tribunal.billing.application.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 接收转单应用层入参（由 订单服务 通过 Feign 调用传入）。
 *
 * <p>对应需求：F-307（转单）。</p>
 *
 * <p>TODO（学习任务）：补充收货地址、运输方式等履约字段。</p>
 */
public record BillReceiveCommand(
        String sourceOrderNo,
        String customerId,
        List<BillLineItem> lines) {

    public record BillLineItem(
            String skuCode,
            String skuName,
            BigDecimal quantity,
            BigDecimal price) {
    }
}
