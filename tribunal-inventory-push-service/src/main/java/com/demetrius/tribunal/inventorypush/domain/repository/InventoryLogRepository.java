package com.demetrius.tribunal.inventorypush.domain.repository;

import com.demetrius.tribunal.inventorypush.domain.model.InventoryLog;

/**
 * 库存流水仓储接口。
 */
public interface InventoryLogRepository {

    void save(InventoryLog log);
}
