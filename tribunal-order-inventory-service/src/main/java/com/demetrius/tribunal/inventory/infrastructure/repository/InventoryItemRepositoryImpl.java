package com.demetrius.tribunal.inventory.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.inventory.domain.model.InventoryItem;
import com.demetrius.tribunal.inventory.domain.model.InventoryItemId;
import com.demetrius.tribunal.inventory.domain.repository.InventoryItemRepository;
import com.demetrius.tribunal.inventory.infrastructure.mapper.InventoryItemMapper;
import com.demetrius.tribunal.inventory.infrastructure.model.InventoryItemPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 库存物料仓储实现（MyBatis-Plus）。
 *
 * <p>乐观锁（N-602/并发超卖防护）：t_inventory_item 表含 version 列，
 * 预占/释放更新时 `updateById` 自动带 `WHERE version=?`，冲突（影响行数 0）抛
 * {@link OptimisticLockConflictException}，由应用层读-改-写重试兜底。</p>
 */
@Repository
public class InventoryItemRepositoryImpl implements InventoryItemRepository {

    /** 乐观锁冲突异常（version 已被其他并发事务 +1，需重读重试）。 */
    public static class OptimisticLockConflictException extends RuntimeException {
        public OptimisticLockConflictException(String skuCode) {
            super("库存乐观锁冲突，请重试: " + skuCode);
        }
    }

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
            return;
        }
        po.setId(exist.getId());
        po.setVersion(item.getVersion());
        int updated = inventoryItemMapper.updateById(po);
        if (updated == 0) {
            throw new OptimisticLockConflictException(item.getSkuCode());
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
    public List<Object> findPage(String skuCode, String skuName, long pageNum, long pageSize) {
        LambdaQueryWrapper<InventoryItemPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(skuCode != null && !skuCode.isBlank(), InventoryItemPo::getSkuCode, skuCode)
               .like(skuName != null && !skuName.isBlank(), InventoryItemPo::getSkuName, skuName)
               .orderByAsc(InventoryItemPo::getSkuCode);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<InventoryItemPo> page =
                inventoryItemMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize), wrapper);
        List<InventoryItem> items = page.getRecords().stream()
                .map(this::toDomain)
                .toList();
        return List.of(page.getTotal(), items);
    }

    @Override
    public void delete(String id) {
        inventoryItemMapper.deleteById(id);
    }

    // ---------- 转换方法（PO ↔ 领域对象） ----------

    private InventoryItem toDomain(InventoryItemPo po) {
        return InventoryItem.restore(
                new InventoryItemId(po.getId()),
                po.getSkuCode(),
                po.getSkuName(),
                po.getUnit(),
                po.getTotalQuantity(),
                po.getReservedQuantity(),
                po.getVersion());
    }

    private InventoryItemPo toPo(InventoryItem item) {
        InventoryItemPo po = new InventoryItemPo();
        po.setId(item.getId().value());
        po.setSkuCode(item.getSkuCode());
        po.setSkuName(item.getSkuName());
        po.setUnit(item.getUnit());
        po.setTotalQuantity(item.getTotalQuantity());
        po.setReservedQuantity(item.getReservedQuantity());
        po.setVersion(item.getVersion());
        return po;
    }
}
