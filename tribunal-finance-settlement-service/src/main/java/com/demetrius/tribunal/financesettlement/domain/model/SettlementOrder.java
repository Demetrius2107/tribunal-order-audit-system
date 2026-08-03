package com.demetrius.tribunal.financesettlement.domain.model;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * 结算单聚合根（对应 settlement_order 表，PRD 5.1）。
 *
 * <p>状态机：PENDING → CHARGING → CHARGED → SPLITTING → SPLIT → SETTLED/CLOSED，
 * 逆向 REFUNDING → REFUNDED（PRD 6.1）。状态流转必须记录流水，禁止跳状态（FR-021）。</p>
 */
@Getter
public class SettlementOrder {

    private final String id;

    /** 结算单号 */
    private final String settlementId;

    /** 关联订单号 */
    private final String orderId;

    private final String userId;

    private final String merchantId;

    /** 状态：PENDING/CHARGING/CHARGED/SPLITTING/SPLIT/REFUNDING/REFUNDED/SETTLED/CLOSED */
    private String status;

    /** 订单总金额 */
    private final BigDecimal totalAmount;

    /** 优惠金额 */
    private final BigDecimal discountAmount;

    /** 运费 */
    private final BigDecimal shippingFee;

    /** 税费 */
    private final BigDecimal taxAmount;

    /** 平台服务费 */
    private final BigDecimal platformFee;

    /** 支付手续费 */
    private final BigDecimal paymentFee;

    /** 实付金额 = total - discount + shipping + tax */
    private final BigDecimal netAmount;

    private final String paymentMethod;

    private final String paymentCurrency;

    /** 支付渠道流水号 */
    private String channelTransactionId;

    public SettlementOrder(String id, String settlementId, String orderId, String userId, String merchantId,
                           String status, BigDecimal totalAmount, BigDecimal discountAmount,
                           BigDecimal shippingFee, BigDecimal taxAmount, BigDecimal platformFee,
                           BigDecimal paymentFee, BigDecimal netAmount, String paymentMethod,
                           String paymentCurrency, String channelTransactionId) {
        this.id = id;
        this.settlementId = settlementId;
        this.orderId = orderId;
        this.userId = userId;
        this.merchantId = merchantId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.shippingFee = shippingFee;
        this.taxAmount = taxAmount;
        this.platformFee = platformFee;
        this.paymentFee = paymentFee;
        this.netAmount = netAmount;
        this.paymentMethod = paymentMethod;
        this.paymentCurrency = paymentCurrency;
        this.channelTransactionId = channelTransactionId;
    }

    /** 扣款成功（更新状态 + 渠道流水号） */
    public void markCharged(String channelTransactionId) {
        this.status = "CHARGED";
        this.channelTransactionId = channelTransactionId;
    }

    /** 扣款失败可重试 */
    public void markChargeFailed() {
        this.status = "CHARGING";
    }
}
