package com.demetrius.tribunal.order.interfaces.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 售后审核请求（接口层 DTO）。
 *
 * @param approved true=通过, false=拒绝
 * @param reason   拒绝原因（approved=false 时必填）
 */
public record AfterSaleReviewRequest(
        @NotNull Boolean approved,
        String reason) {
}
