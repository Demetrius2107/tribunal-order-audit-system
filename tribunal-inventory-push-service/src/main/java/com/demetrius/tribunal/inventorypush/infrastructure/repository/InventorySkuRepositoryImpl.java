package com.demetrius.tribunal.inventorypush.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.inventorypush.domain.model.InventorySku;
import com.demetrius.tribunal.inventorypush.domain.repository.InventorySkuRepository;
import com.demetrius.tribunal.inventorypush.infrastructure.mapper.InventorySkuMapper;
import com.demetrius.tribunal.inventorypush.infrastructure.model.InventorySkuPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 库存主数据仓储实现（infrastructure 层，PO ↔ Domain 转换）。
 */
@Repository
public class InventorySkuRepositoryImpl implements InventorySkuRepository {

    private final InventorySkuMapper inventorySkuMapper;

    public InventorySkuRepositoryImpl(InventorySkuMapper inventorySkuMapper) {
        this.inventorySkuMapper = inventorySkuMapper;
    }

    @Override
    public Optional<InventorySku> findBySkuWarehouseOwner(String skuId, String warehouseId, String ownerId) {
        InventorySkuPo po = inventorySkuMapper.selectOne(new LambdaQueryWrapper<InventorySkuPo>()
                .eq(InventorySkuPo::getSkuId, skuId)
                .eq(InventorySkuPo::getWarehouseId, warehouseId)
                .eq(InventorySkuPo::getOwnerId, ownerId));
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public void save(InventorySku sku) {
        InventorySkuPo po = toPo(sku);
        if (inventorySkuMapper.selectById(po.getId()) == null) {
            inventorySkuMapper.insert(po);
        } else {
            inventorySkuMapper.updateById(po);
        }
    }

    @Override
    public boolean updateWithVersion(InventorySku sku) {
        // MyBatis-Plus 乐观锁：PO 带 @Version 后 updateById 失败返回 0
        return inventorySkuMapper.updateById(toPo(sku)) > 0;
    }

    private InventorySku toDomain(InventorySkuPo po) {
        return new InventorySku(
                po.getId(), po.getSkuId(), po.getWarehouseId(), po.getOwnerId(),
                po.getTotalQty() == null ? 0 : po.getTotalQty(),
                po.getAvailableQty() == null ? 0 : po.getAvailableQty(),
                po.getLockedQty() == null ? 0 : po.getLockedQty(),
                po.getInTransitQty() == null ? 0 : po.getInTransitQty(),
                po.getReservedQty() == null ? 0 : po.getReservedQty(),
                po.getVersion() == null ? 0 : po.getVersion());
    }

    private InventorySkuPo toPo(InventorySku sku) {
        InventorySkuPo po = new InventorySkuPo();
        po.setId(sku.getId());
        po.setSkuId(sku.getSkuId());
        po.setWarehouseId(sku.getWarehouseId());
        po.setOwnerId(sku.getOwnerId());
        po.setTotalQty(sku.getTotalQty());
        po.setAvailableQty(sku.getAvailableQty());
        po.setLockedQty(sku.getLockedQty());
        po.setInTransitQty(sku.getInTransitQty());
        po.setReservedQty(sku.getReservedQty());
        po.setVersion(sku.getVersion());
        return po;
    }
}
