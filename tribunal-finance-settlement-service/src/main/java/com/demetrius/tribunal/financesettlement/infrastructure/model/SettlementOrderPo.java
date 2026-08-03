package com.demetrius.tribunal.financesettlement.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 结算单主表持久化对象（对应 settlement_order 表，PRD 5.1）。
 */
@Data
@TableName("settlement_order")
public class SettlementOrderPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 结算单号 */
    private String settlementId;

    /** 关联订单号 */
    private String orderId;

    private String userId;

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

    private String paymentMethod;

    private String paymentCurrency;

    /** 支付渠道流水号 */
    private String channelTransactionId;
}
