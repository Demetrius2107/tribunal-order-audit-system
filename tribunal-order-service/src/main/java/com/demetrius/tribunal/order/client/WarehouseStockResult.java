package com.demetrius.tribunal.order.client;

import java.math.BigDecimal;

/**
 * M4：仓库级库存查询结果（inventory-service → order-service）。
 *
 * <p>表示某仓库中某 SKU 的可售/可发货库存，供寻源分仓使用。</p>
 *
 * @param warehouseId       仓库 ID
 * @param skuCode           SKU 编码
 * @param availableQuantity 可用库存
 */
public record WarehouseStockResult(
        String warehouseId,
        String skuCode,
        BigDecimal availableQuantity) {
}
