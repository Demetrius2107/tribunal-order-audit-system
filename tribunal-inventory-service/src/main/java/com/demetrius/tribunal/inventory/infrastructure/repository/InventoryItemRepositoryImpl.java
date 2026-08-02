package com.demetrius.tribunal.inventory.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.inventory.domain.model.InventoryItem;
import com.demetrius.tribunal.inventory.domain.model.InventoryItemId;
import com.demetrius.tribunal.inventory.domain.repository.InventoryItemRepository;
import com.demetrius.tribunal.inventory.infrastructure.mapper.InventoryItemMapper;
import com.demetrius.tribunal.inventory.infrastructure.model.InventoryItemPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 库存物料仓储实现（MyBatis-Plus）。
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>乐观锁：PO 加 @Version 防止并发预占超卖</li>
 *   <li>库存变动流水：预占/释放写流水表（审计 + 对账）</li>
 * </ul>
 */
@Repository
public class InventoryItemRepositoryImpl implements InventoryItemRepository {

    private final InventoryItemMapper inventoryItemMapper;

    public InventoryItemRepositoryImpl(InventoryItemMapper inventoryItemMapper) {
        this.inventoryItemMapper = inventoryItemMapper;
    }

    @Override
    public void save(InventoryItem item) {
        InventoryItemPo po = toPo(item);
        InventoryItemPo exist = inventoryItemMapper.selectOne(
                new LambdaQueryWrapper<InventoryItemPo>().eq(InventoryItemPo::getSkuCode, item.getSkuCode()));
        if (exist == null) {
            inventoryItemMapper.insert(po);
        } else {
            po.setId(exist.getId());
            inventoryItemMapper.updateById(po);
        }
    }

    @Override
    public Optional<InventoryItem> findById(String id) {
        InventoryItemPo po = inventoryItemMapper.selectById(id);
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public Optional<InventoryItem> findBySkuCode(String skuCode) {
        InventoryItemPo po = inventoryItemMapper.selectOne(
                new LambdaQueryWrapper<InventoryItemPo>().eq(InventoryItemPo::getSkuCode, skuCode));
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public void delete(String id) {
        inventoryItemMapper.deleteById(id);
    }

    // ---------- 转换方法（PO ↔ 领域对象） ----------

    private InventoryItem toDomain(InventoryItemPo po) {
        return new InventoryItem(
                new InventoryItemId(po.getId()),
                po.getSkuCode(),
                po.getSkuName(),
                po.getUnit(),
                po.getTotalQuantity(),
                po.getReservedQuantity());
    }

    private InventoryItemPo toPo(InventoryItem item) {
        InventoryItemPo po = new InventoryItemPo();
        po.setId(item.getId().value());
        po.setSkuCode(item.getSkuCode());
        po.setSkuName(item.getSkuName());
        po.setUnit(item.getUnit());
        po.setTotalQuantity(item.getTotalQuantity());
        po.setReservedQuantity(item.getReservedQuantity());
        return po;
    }
}
