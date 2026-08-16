package com.demetrius.tribunal.inventory.domain.repository;

import com.demetrius.tribunal.inventory.domain.model.InventoryItem;

import java.util.List;
import java.util.Optional;

/**
 * 库存物料仓储接口（domain 定义，infrastructure 实现）。
 */
public interface InventoryItemRepository {

    void save(InventoryItem item);

    Optional<InventoryItem> findById(String id);

    Optional<InventoryItem> findBySkuCode(String skuCode);

    /**
     * SKU 分页查询（F-101：按编码/名称模糊检索，可空过滤）。
     *
     * @return [总数, 本页列表]
     */
    List<Object> findPage(String skuCode, String skuName, long pageNum, long pageSize);

    void delete(String id);
}
