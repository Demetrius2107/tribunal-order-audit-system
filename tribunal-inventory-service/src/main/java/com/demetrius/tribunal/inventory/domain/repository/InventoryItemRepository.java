package com.demetrius.tribunal.inventory.domain.repository;

import com.demetrius.tribunal.inventory.domain.model.InventoryItem;

import java.util.Optional;

/**
 * 库存物料仓储接口（domain 定义，infrastructure 实现）。
 *
 * <p>TODO（学习任务）：补充分页查询与按条件查询（SKU 编码模糊/状态）。</p>
 */
public interface InventoryItemRepository {

    void save(InventoryItem item);

    Optional<InventoryItem> findById(String id);

    Optional<InventoryItem> findBySkuCode(String skuCode);

    void delete(String id);
}
