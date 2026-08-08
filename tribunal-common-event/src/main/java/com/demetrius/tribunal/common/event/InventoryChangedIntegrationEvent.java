package com.demetrius.tribunal.common.event;

/**
 * 库存变更事件（inventory-push-service 发布 → order-inventory-service 订阅同步）。
 *
 * <p>R1 契约占位；M3 异步化时补充批次/预占/可用量变更详情字段。</p>
 */
public record InventoryChangedIntegrationEvent(
        String eventId,
        long occurredAt,
        String skuId,
        String warehouseId,
        long availableQty
) implements IntegrationEvent {
}
