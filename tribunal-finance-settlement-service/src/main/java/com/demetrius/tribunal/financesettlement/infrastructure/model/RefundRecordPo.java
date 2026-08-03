package com.demetrius.tribunal.financesettlement.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 退款记录表持久化对象（对应 refund_record 表，PRD 5.1）。
 */
@Data
@TableName("refund_record")
public class RefundRecordPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 退款单号 */
    private String refundId;

    private String settlementId;

    private String originalOrderId;

    /** 退款类型：FULL/PARTIAL */
    private String refundType;

    private BigDecimal refundAmount;

    private String reason;

    private String reasonCode;

    /** 状态：PENDING/APPROVED/REJECTED/PROCESSING/SUCCESS/FAILED */
    private String status;

    /** 审核人 */
    private String approverId;

    private String channelTransactionId;
}
