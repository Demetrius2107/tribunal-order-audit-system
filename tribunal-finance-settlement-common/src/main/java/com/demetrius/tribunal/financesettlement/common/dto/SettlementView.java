package com.demetrius.tribunal.financesettlement.common.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 结算单视图（结算单主表对外回显，对应 PRD 5.1 settlement_order 表）。
 */
@Data
public class SettlementView {

    /** 结算单号 */
    private String settlementId;

    /** 关联订单号 */
    private String orderId;

    /** 商家 ID */
    private String merchantId;

    /** 状态：PENDING/CHARGING/CHARGED/SPLITTING/SPLIT/REFUNDING/REFUNDED/SETTLED/CLOSED */
    private String status;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 优惠金额 */
    private BigDecimal discountAmount;

    /** 运费 */
    private BigDecimal shippingFee;

    /** 税费 */
    private BigDecimal taxAmount;

    /** 平台服务费 */
    private BigDecimal platformFee;

    /** 支付手续费 */
    private BigDecimal paymentFee;

    /** 实付金额 = total - discount + shipping + tax */
    private BigDecimal netAmount;

    /** 支付方式 */
    private String paymentMethod;

    /** 支付币种 */
    private String paymentCurrency;

    /** 支付渠道流水号 */
    private String channelTransactionId;
}
