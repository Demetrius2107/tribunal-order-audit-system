package com.demetrius.tribunal.inventory.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demetrius.tribunal.inventory.application.dto.InventoryFlowPage;
import com.demetrius.tribunal.inventory.application.dto.InventoryFlowResult;
import com.demetrius.tribunal.inventory.infrastructure.mapper.InventoryFlowMapper;
import com.demetrius.tribunal.inventory.infrastructure.model.InventoryFlowPo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 库存变动流水查询应用服务（对外报表：流水分页查询）。
 */
@Service
public class InventoryFlowQueryApplicationService {

    private final InventoryFlowMapper inventoryFlowMapper;

    public InventoryFlowQueryApplicationService(InventoryFlowMapper inventoryFlowMapper) {
        this.inventoryFlowMapper = inventoryFlowMapper;
    }

    /**
     * 流水分页查询（可按 SKU/变动类型过滤，时间倒序）。
     */
    @Transactional(readOnly = true)
    public InventoryFlowPage query(String skuCode, String changeType, long pageNum, long pageSize) {
        LambdaQueryWrapper<InventoryFlowPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(skuCode != null && !skuCode.isBlank(), InventoryFlowPo::getSkuCode, skuCode)
               .eq(changeType != null && !changeType.isBlank(), InventoryFlowPo::getChangeType, changeType)
               .orderByDesc(InventoryFlowPo::getCreateTime);

        Page<InventoryFlowPo> page = inventoryFlowMapper.selectPage(
                new Page<>(pageNum, pageSize), wrapper);
        List<InventoryFlowResult> flows = page.getRecords().stream()
                .map(InventoryFlowResult::from)
                .toList();
        return InventoryFlowPage.of(page.getTotal(), pageNum, pageSize, flows);
    }
}
