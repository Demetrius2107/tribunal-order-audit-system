package com.demetrius.tribunal.inventorypush.domain.model;

import lombok.Getter;

/**
 * 幂等记录领域实体（对应 idempotent_record 表，PRD 5.1）。
 *
 * <p>幂等键：batchId_skuId_warehouseId_version，有效期 7 天（PRD 2.5.1 FR-047）。</p>
 */
@Getter
public class IdempotentRecord {

    /** 幂等键 */
    private final String idempotencyKey;

    /** 上游推送批次号 */
    private final String batchId;

    /** 状态：SUCCESS/FAILED */
    private String status;

    /** 过期时间 */
    private final String expireAt;

    public IdempotentRecord(String idempotencyKey, String batchId, String status, String expireAt) {
        this.idempotencyKey = idempotencyKey;
        this.batchId = batchId;
        this.status = status;
        this.expireAt = expireAt;
    }
}
