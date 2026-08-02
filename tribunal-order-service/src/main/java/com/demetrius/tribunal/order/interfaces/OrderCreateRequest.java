package com.demetrius.tribunal.order.interfaces.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * 下单接口请求 DTO（接口层）。
 *
 * <p>对照旧项目：{@code OrderController} 的下单接口入参。</p>
 * <p>接口层 DTO 只做参数接收与校验，不包含业务逻辑；随后转换为应用层 Command。</p>
 */
public record OrderCreateRequest(

        @NotEmpty(message = "客户ID不能为空")
        String customerId,

        @NotEmpty(message = "订单明细不能为空")
        @Valid
        List<SkuItemRequest> skus) {

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
