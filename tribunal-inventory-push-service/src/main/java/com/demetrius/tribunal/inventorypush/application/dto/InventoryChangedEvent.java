package com.demetrius.tribunal.inventorypush.application.dto;

/**
 * 库存变更事件（对应 PRD 4.2 向下游分发报文 / FR-036 事件订阅模式）。
 *
 * <p>库存推送系统发布到 topic: inventory-events，下游订单系统订阅消费。</p>
 */
public record InventoryChangedEvent(
        String messageId,
        String eventType,
        String timestamp,
        String skuId,
        String warehouseId,
        String ownerId,
        Inventory inventory,
        ChangeDetail changeDetail,
        BatchInfo batchInfo) {

    /**
     * 库存快照。
     */
    public record Inventory(
            Integer availableQty,
            Integer totalQty,
            Integer lockedQty,
            Integer inTransitQty) {
    }

    /**
     * 变动明细。
     */
    public record ChangeDetail(
            String field,
            Integer oldValue,
            Integer newValue,
            Integer delta) {
    }

    /**
     * 批次信息。
     */
    public record BatchInfo(
            String batchNo,
            String expiryDate) {
    }
}
