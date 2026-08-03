package com.demetrius.tribunal.inventorypush.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.inventorypush.domain.model.InventoryBatch;
import com.demetrius.tribunal.inventorypush.domain.repository.InventoryBatchRepository;
import com.demetrius.tribunal.inventorypush.infrastructure.mapper.InventoryBatchMapper;
import com.demetrius.tribunal.inventorypush.infrastructure.model.InventoryBatchPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 批次库存仓储实现（infrastructure 层）。
 */
@Repository
public class InventoryBatchRepositoryImpl implements InventoryBatchRepository {

    private final InventoryBatchMapper inventoryBatchMapper;

    public InventoryBatchRepositoryImpl(InventoryBatchMapper inventoryBatchMapper) {
        this.inventoryBatchMapper = inventoryBatchMapper;
    }

    @Override
    public Optional<InventoryBatch> findBySkuWarehouseBatch(String skuId, String warehouseId, String batchNo) {
        InventoryBatchPo po = inventoryBatchMapper.selectOne(new LambdaQueryWrapper<InventoryBatchPo>()
                .eq(InventoryBatchPo::getSkuId, skuId)
                .eq(InventoryBatchPo::getWarehouseId, warehouseId)
                .eq(InventoryBatchPo::getBatchNo, batchNo));
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public void save(InventoryBatch batch) {
        InventoryBatchPo po = new InventoryBatchPo();
        po.setId(batch.getId());
        po.setSkuId(batch.getSkuId());
        po.setWarehouseId(batch.getWarehouseId());
        po.setBatchNo(batch.getBatchNo());
        po.setProductionDate(batch.getProductionDate());
        po.setExpiryDate(batch.getExpiryDate());
        po.setQty(batch.getQty());
        po.setStatus(batch.getStatus());
        if (inventoryBatchMapper.selectById(po.getId()) == null) {
            inventoryBatchMapper.insert(po);
        } else {
            inventoryBatchMapper.updateById(po);
        }
    }

    private InventoryBatch toDomain(InventoryBatchPo po) {
        return new InventoryBatch(
                po.getId(), po.getSkuId(), po.getWarehouseId(), po.getBatchNo(),
                po.getProductionDate(), po.getExpiryDate(),
                po.getQty() == null ? 0 : po.getQty(), po.getStatus());
    }
}
