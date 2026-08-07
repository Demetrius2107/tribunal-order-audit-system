package com.demetrius.tribunal.common.dto.finance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 扣款请求（对应 PRD 4.2 POST /api/v1/payment/charge）。
 *
 * <p>幂等键：结算单号 + 扣款批次号（FR-017），杜绝重复扣款（FR-015 核心红线）。</p>
 */
@Data
public class ChargeRequest {

    /** 结算单号 */
    @NotBlank(message = "settlementId 不能为空")
    private String settlementId;

    /** 幂等键：SET_xxx_BATCH_n */
    @NotBlank(message = "idempotencyKey 不能为空")
    private String idempotencyKey;

    /** 扣款金额（分） */
    @NotNull(message = "amount 不能为空")
    private BigDecimal amount;

    /** 币种，默认 CNY */
    private String currency;

    /** 支付方式：WECHAT_PAY/ALIPAY/UNIONPAY/APPLE_PAY */
    @NotBlank(message = "paymentMethod 不能为空")
    private String paymentMethod;

    /** 用户支付凭证 */
    private String paymentToken;

    /** 扣款说明 */
    private String description;

    /** 关联订单号（元数据） */
    private String orderId;

    /** 关联用户 ID（元数据） */
    private String userId;
}
