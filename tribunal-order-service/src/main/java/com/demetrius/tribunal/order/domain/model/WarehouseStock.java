package com.demetrius.tribunal.order.domain.model;

import java.math.BigDecimal;

/**
 * M4：仓库库存值对象（寻源服务的数据单元）。
 *
 * <p>表示某个仓库中某 SKU 的可用库存，由 inventory-service 通过 Feign 查询回填，
 * 供 {@code WarehouseRoutingService} 判断是否可发货。</p>
 *
 * @param warehouseId       仓库 ID
 * @param skuCode           SKU 编码
 * @param availableQuantity 可用库存（可销售/可发货）
 */
public record WarehouseStock(String warehouseId, String skuCode, BigDecimal availableQuantity) {
}
