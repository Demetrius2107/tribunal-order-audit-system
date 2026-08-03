package com.demetrius.tribunal.financesettlement.domain.model;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * 分账记录实体（对应 split_record 表，PRD 5.1）。
 *
 * <p>分账方类型：MERCHANT/PLATFORM/LOGISTICS/AGENT（FR-024），比例之和 = 100%（FR-025）。</p>
 */
@Getter
public class SplitRecord {

    private final String id;

    private final String settlementId;

    /** 收款方 ID */
    private final String recipientId;

    /** 收款方类型 */
    private final String recipientType;

    /** 分账金额 */
    private final BigDecimal splitAmount;

    /** 分账比例 */
    private final BigDecimal splitRate;

    /** 状态：PENDING/SUCCESS/FAILED */
    private String status;

    private final String channelTransactionId;

    public SplitRecord(String id, String settlementId, String recipientId, String recipientType,
                       BigDecimal splitAmount, BigDecimal splitRate, String status, String channelTransactionId) {
        this.id = id;
        this.settlementId = settlementId;
        this.recipientId = recipientId;
        this.recipientType = recipientType;
        this.splitAmount = splitAmount;
        this.splitRate = splitRate;
        this.status = status;
        this.channelTransactionId = channelTransactionId;
    }
}
