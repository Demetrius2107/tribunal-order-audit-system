package com.demetrius.tribunal.inventorypush.application.service;

import com.demetrius.tribunal.inventorypush.common.dto.InventorySkuView;
import com.demetrius.tribunal.inventorypush.common.exception.BizException;
import com.demetrius.tribunal.inventorypush.domain.model.InventorySku;
import com.demetrius.tribunal.inventorypush.domain.repository.InventorySkuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;

/**
 * 库存查询应用服务（PRD 2.4.1 FR-035 被动查询 Pull）。
 */
@Service
public class InventoryQueryApplicationService {

    private final InventorySkuRepository inventorySkuRepository;

    public InventoryQueryApplicationService(InventorySkuRepository inventorySkuRepository) {
        this.inventorySkuRepository = inventorySkuRepository;
    }

    /**
     * 查询 SKU 实时库存状态（对应 PRD 4.3 GET /api/v1/inventory/query）。
     */
    @Transactional(readOnly = true)
    public InventorySkuView query(String skuId, String warehouseId, String ownerId) {
        InventorySku sku = inventorySkuRepository
                .findBySkuWarehouseOwner(skuId, warehouseId, ownerId)
                .orElseThrow(() -> new BizException("INV-005", "SKU 不存在: " + skuId));

        InventorySkuView view = new InventorySkuView();
        view.setSkuId(sku.getSkuId());
        view.setWarehouseId(sku.getWarehouseId());
        view.setAvailableQty(sku.getAvailableQty());
        view.setTotalQty(sku.getTotalQty());
        // 批次明细查询（InventoryBatchRepository）留待后续按 PRD 2.3.3 补全
        view.setBatches(new ArrayList<>());
        return view;
    }
}
