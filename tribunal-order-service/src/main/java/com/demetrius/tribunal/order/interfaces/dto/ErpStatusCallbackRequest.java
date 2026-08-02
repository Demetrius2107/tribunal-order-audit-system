package com.demetrius.tribunal.order.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * ERP 状态回传请求 DTO（erp-service → order-service）。
 *
 * <p>对应需求：F-308（状态回传）、N-304（回传幂等）。</p>
 */
public record ErpStatusCallbackRequest(

        /** 来源订单编号（订单的 orderNo） */
        @NotBlank(message = "订单编号不能为空")
        String sourceOrderNo,

        /** ERP 履约状态（SHIPPED/SIGNED/CLOSED/CANCELLED） */
        @NotBlank(message = "履约状态不能为空")
        String erpStatus,

        /** 履约单号（ERP 侧） */
        String erpOrderId) {
}
