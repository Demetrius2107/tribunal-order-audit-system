package com.demetrius.tribunal.marketing.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 领券请求（接口层 DTO）。
 *
 * @param customerId 领用人 ID
 */
public record CouponReceiveRequest(
        @NotBlank String customerId) {
}
