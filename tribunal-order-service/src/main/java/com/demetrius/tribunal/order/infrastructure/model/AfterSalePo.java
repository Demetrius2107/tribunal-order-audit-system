package com.demetrius.tribunal.order.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 售后单持久化对象（对应 t_after_sale 表）。
 */
@Data
@TableName("t_after_sale")
public class AfterSalePo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 售后单号（业务唯一键） */
    private String afterSaleNo;

    /** 原订单 ID */
    private String orderId;

    /** 原订单编号 */
    private String orderNo;

    private String customerId;

    /** 售后类型（RETURN_REFUND / REFUND_ONLY） */
    private String type;

    /** 售后原因（QUALITY_ISSUE / DAMAGED / ...） */
    private String reason;

    /** 状态（与 AfterSaleStatus.name() 对应） */
    private String status;

    /** 退款总额（商品退款 + 押金退还） */
    private BigDecimal totalRefundAmount;

    /** 拒绝原因 */
    private String rejectReason;

    /** 退款流水号 */
    private String refundTxnNo;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
