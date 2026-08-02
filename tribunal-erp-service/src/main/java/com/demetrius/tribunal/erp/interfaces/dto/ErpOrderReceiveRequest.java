package com.demetrius.tribunal.erp.interfaces.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * ERP 接收转单接口请求 DTO（由 OMS Feign 调用）。
 */
public record ErpOrderReceiveRequest(

        @NotEmpty(message = "来源订单编号不能为空")
        String sourceOrderNo,

        @NotEmpty(message = "客户ID不能为空")
        String customerId,

        @NotEmpty(message = "履约明细不能为空")
        @Valid
        List<ErpOrderLineItemRequest> lines) {

    public record ErpOrderLineItemRequest(
            @NotEmpty(message = "SKU编码不能为空")
            String skuCode,
            String skuName,
            @NotNull(message = "数量不能为空")
            BigDecimal quantity,
            @NotNull(message = "单价不能为空")
            BigDecimal price) {
    }
}
