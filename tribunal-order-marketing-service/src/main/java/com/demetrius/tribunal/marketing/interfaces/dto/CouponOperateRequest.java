package com.demetrius.tribunal.marketing.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 核销/锁定/释放券请求（接口层 DTO）。
 *
 * @param couponCode 券码
 * @param orderId    关联订单 ID
 */
public record CouponOperateRequest(
        @NotBlank String couponCode,
        @NotBlank String orderId) {
}
