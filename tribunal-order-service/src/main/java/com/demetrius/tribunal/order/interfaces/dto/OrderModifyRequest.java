package com.demetrius.tribunal.order.interfaces.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 改单接口请求 DTO（F-309：仅待确认订单可改，替换明细 + 整托规格）。
 */
public record OrderModifyRequest(

        @NotEmpty(message = "订单明细不能为空")
        @Valid
        List<OrderCreateRequest.SkuItemRequest> skus,

        /** 整托规格表：SKU编码 → 每托数量（可选） */
        Map<String, BigDecimal> palletSpecs) {
}
