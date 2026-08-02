package com.demetrius.tribunal.order.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 审单接口请求 DTO（接口层）。
 *
 * <p>对照旧项目：{@code SalesmanController.reviewOrder} 审核订单接口入参。</p>
 */
public record OrderReviewRequest(

        @NotBlank(message = "订单ID不能为空")
        String orderId,

        /** true=通过，false=拒绝 */
        boolean approved,

        /** 拒绝原因（拒绝时必填，TODO：分组校验） */
        String reason,

        /** 操作人（TODO：应从登录态获取，对照旧项目 @CurrentUser） */
        String operator) {
}
