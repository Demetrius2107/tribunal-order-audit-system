package com.demetrius.tribunal.inventorypush.domain.repository;

import com.demetrius.tribunal.inventorypush.domain.model.InventorySku;

import java.util.Optional;

/**
 * 库存主数据仓储接口（domain 定义，infrastructure 实现）。
 */
public interface InventorySkuRepository {

    Optional<InventorySku> findBySkuWarehouseOwner(String skuId, String warehouseId, String ownerId);

    void save(InventorySku sku);

    /** 乐观锁更新：版本号不匹配时返回 false，由应用层重试（PRD 6.2 重试 3 次） */
    boolean updateWithVersion(InventorySku sku);
}
