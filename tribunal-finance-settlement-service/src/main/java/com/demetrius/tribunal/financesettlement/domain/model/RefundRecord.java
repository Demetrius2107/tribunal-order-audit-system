package com.demetrius.tribunal.financesettlement.domain.model;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * 退款记录实体（对应 refund_record 表，PRD 5.1）。
 *
 * <p>状态机：PENDING → APPROVED → PROCESSING → SUCCESS/FAILED（PRD 2.4.3 FR-045）。</p>
 */
@Getter
public class RefundRecord {

    private final String id;

    /** 退款单号 */
    private final String refundId;

    private final String settlementId;

    private final String originalOrderId;

    /** 退款类型：FULL/PARTIAL */
    private final String refundType;

    /** 退款金额 */
    private final BigDecimal refundAmount;

    private final String reason;

    private final String reasonCode;

    /** 状态：PENDING/APPROVED/REJECTED/PROCESSING/SUCCESS/FAILED */
    private String status;

    private final String approverId;

    private final String channelTransactionId;

    public RefundRecord(String id, String refundId, String settlementId, String originalOrderId,
                        String refundType, BigDecimal refundAmount, String reason, String reasonCode,
                        String status, String approverId, String channelTransactionId) {
        this.id = id;
        this.refundId = refundId;
        this.settlementId = settlementId;
        this.originalOrderId = originalOrderId;
        this.refundType = refundType;
        this.refundAmount = refundAmount;
        this.reason = reason;
        this.reasonCode = reasonCode;
        this.status = status;
        this.approverId = approverId;
        this.channelTransactionId = channelTransactionId;
    }
}
