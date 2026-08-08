package com.demetrius.tribunal.order.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 合单发货请求（接口层 DTO）。
 *
 * @param trackingNo 物流单号
 */
public record MergeOrderShipRequest(
        @NotBlank String trackingNo) {
}
