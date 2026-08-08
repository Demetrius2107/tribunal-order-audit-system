package com.demetrius.tribunal.marketing.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建券模板请求（接口层 DTO）。
 */
public record CouponTemplateCreateRequest(
        @NotBlank String name,
        @NotNull String type,
        BigDecimal threshold,
        BigDecimal deductionAmount,
        BigDecimal discountRate,
        Integer totalQuota,
        @Positive Integer perUserLimit,
        LocalDateTime validStartTime,
        LocalDateTime validEndTime) {
}
