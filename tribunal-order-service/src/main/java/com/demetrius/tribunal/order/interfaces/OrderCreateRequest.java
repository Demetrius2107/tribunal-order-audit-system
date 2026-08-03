package com.demetrius.tribunal.order.interfaces.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 下单接口请求 DTO（接口层）。
 *
 * <p>接口层 DTO 只做参数接收与校验，不包含业务逻辑；随后转换为应用层 Command。</p>
 */
public record OrderCreateRequest(

        @NotEmpty(message = "客户ID不能为空")
        String customerId,

        @NotEmpty(message = "订单明细不能为空")
        @Valid
        List<SkuItemRequest> skus,

        /** 整托规格表：SKU编码 → 每托数量（可选；未配置规格的 SKU 不做整托校验） */
        Map<String, BigDecimal> palletSpecs) {

    public record SkuItemRequest(
            @NotEmpty(message = "SKU编码不能为空")
            String skuCode,
            String skuName,
            @NotNull(message = "数量不能为空")
            BigDecimal quantity,
            @NotNull(message = "单价不能为空")
            BigDecimal price) {
    }
}
