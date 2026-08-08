package com.demetrius.tribunal.order.interfaces.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * 发起售后请求（接口层 DTO）。
 */
public record AfterSaleCreateRequest(
        @NotBlank String orderId,
        @NotNull String type,
        @NotNull String reason,
        @NotEmpty @Valid List<ReturnItem> items) {

    public record ReturnItem(
            @NotBlank String skuCode,
            @NotNull BigDecimal quantity) {
    }
}
