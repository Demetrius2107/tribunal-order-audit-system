package com.demetrius.tribunal.inventorypush.domain.repository;

import com.demetrius.tribunal.inventorypush.domain.model.InventoryBatch;

import java.util.Optional;

/**
 * 批次库存仓储接口。
 */
public interface InventoryBatchRepository {

    Optional<InventoryBatch> findBySkuWarehouseBatch(String skuId, String warehouseId, String batchNo);

    void save(InventoryBatch batch);
}
