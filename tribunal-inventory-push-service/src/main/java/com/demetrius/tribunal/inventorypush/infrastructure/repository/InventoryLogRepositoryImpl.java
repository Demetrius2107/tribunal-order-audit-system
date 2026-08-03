package com.demetrius.tribunal.inventorypush.infrastructure.repository;

import com.demetrius.tribunal.inventorypush.domain.model.InventoryLog;
import com.demetrius.tribunal.inventorypush.domain.repository.InventoryLogRepository;
import com.demetrius.tribunal.inventorypush.infrastructure.mapper.InventoryLogMapper;
import com.demetrius.tribunal.inventorypush.infrastructure.model.InventoryLogPo;
import org.springframework.stereotype.Repository;

/**
 * 库存流水仓储实现（infrastructure 层）。
 */
@Repository
public class InventoryLogRepositoryImpl implements InventoryLogRepository {

    private final InventoryLogMapper inventoryLogMapper;

    public InventoryLogRepositoryImpl(InventoryLogMapper inventoryLogMapper) {
        this.inventoryLogMapper = inventoryLogMapper;
    }

    @Override
    public void save(InventoryLog log) {
        InventoryLogPo po = new InventoryLogPo();
        po.setId(log.getId());
        po.setSkuId(log.getSkuId());
        po.setWarehouseId(log.getWarehouseId());
        po.setOwnerId(log.getOwnerId());
        po.setChangeType(log.getChangeType());
        po.setDeltaQty(log.getDeltaQty());
        po.setBeforeQty(log.getBeforeQty());
        po.setAfterQty(log.getAfterQty());
        po.setBatchId(log.getBatchId());
        po.setSourceBatchId(log.getSourceBatchId());
        po.setMessageId(log.getMessageId());
        inventoryLogMapper.insert(po);
    }
}
