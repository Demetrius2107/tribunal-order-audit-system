package com.demetrius.tribunal.financesettlement.domain.model;

import lombok.Getter;

/**
 * 扣款幂等记录实体（对应 payment_idempotent 表，PRD 5.1）。
 *
 * <p>幂等键：settlementId_batchNo（FR-017），"最多扣一次"红线（NFR-008）。</p>
 */
@Getter
public class PaymentIdempotent {

    /** 幂等键：settlementId_batchNo */
    private final String idempotencyKey;

    private final String settlementId;

    /** 状态：SUCCESS/FAILED/PROCESSING */
    private final String status;

    /** 渠道原始响应 */
    private final String channelResponse;

    /** 过期时间 */
    private final String expireAt;

    public PaymentIdempotent(String idempotencyKey, String settlementId, String status,
                             String channelResponse, String expireAt) {
        this.idempotencyKey = idempotencyKey;
        this.settlementId = settlementId;
        this.status = status;
        this.channelResponse = channelResponse;
        this.expireAt = expireAt;
    }
}
