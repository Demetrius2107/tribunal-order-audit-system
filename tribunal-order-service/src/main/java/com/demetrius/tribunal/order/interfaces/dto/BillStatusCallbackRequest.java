package com.demetrius.tribunal.order.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 账单状态回传请求 DTO（billing-service → order-service）。
 *
 * <p>对应需求：F-308（状态回传）、N-304（回传幂等）。</p>
 */
public record BillStatusCallbackRequest(

        /** 来源订单编号（订单的 orderNo） */
        @NotBlank(message = "订单编号不能为空")
        String sourceOrderNo,

        /** 账单状态（SHIPPED/SIGNED/CLOSED/CANCELLED） */
        @NotBlank(message = "账单状态不能为空")
        String billStatus,

        /** 账单号（账单模块侧） */
        String billId) {
}
