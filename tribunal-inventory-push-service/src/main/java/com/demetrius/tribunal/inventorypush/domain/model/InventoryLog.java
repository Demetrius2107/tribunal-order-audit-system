package com.demetrius.tribunal.inventorypush.domain.model;

import lombok.Getter;

/**
 * 库存流水领域实体（对应 inventory_log 表，PRD 5.1）。
 *
 * <p>记录每次库存变动的前后值与变动类型，支撑追溯（PRD 2.3.1 FR-025）。</p>
 */
@Getter
public class InventoryLog {

    private final String id;

    private final String skuId;

    private final String warehouseId;

    private final String ownerId;

    /** 变动类型：PUSH/LOCK/UNLOCK/RESERVE */
    private final String changeType;

    /** 变动数量（可正可负） */
    private final int deltaQty;

    private final int beforeQty;

    private final int afterQty;

    /** 关联批次号 */
    private final String batchId;

    /** 上游推送批次号 */
    private final String sourceBatchId;

    /** 下游分发消息 ID */
    private final String messageId;

    public InventoryLog(String id, String skuId, String warehouseId, String ownerId,
                        String changeType, int deltaQty, int beforeQty, int afterQty,
                        String batchId, String sourceBatchId, String messageId) {
        this.id = id;
        this.skuId = skuId;
        this.warehouseId = warehouseId;
        this.ownerId = ownerId;
        this.changeType = changeType;
        this.deltaQty = deltaQty;
        this.beforeQty = beforeQty;
        this.afterQty = afterQty;
        this.batchId = batchId;
        this.sourceBatchId = sourceBatchId;
        this.messageId = messageId;
    }
}
