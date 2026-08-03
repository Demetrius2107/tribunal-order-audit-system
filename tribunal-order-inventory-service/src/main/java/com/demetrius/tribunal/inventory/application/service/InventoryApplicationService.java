package com.demetrius.tribunal.inventory.application.service;

import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.inventory.domain.model.InventoryItem;
import com.demetrius.tribunal.inventory.domain.repository.InventoryItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 库存物料应用服务（用例编排层）。
 *
 * <p>对应需求：F-501（库存查询）、F-502（库存预占/释放）、库存物料推送上游。</p>
 *
 * <p>职责：</p>
 * <ol>
 *   <li>物料主数据维护（新增/更新库存）</li>
 *   <li>库存查询：供订单服务下单校验可售量</li>
 *   <li>预占/释放：下单预占库存，取消/签收释放</li>
 *   <li>推送上游：物料/库存变化时推送订单服务（F-501 数据来源）</li>
 * </ol>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>库存变动流水：每次预占/释放写流水表（审计 + 对账）</li>
 *   <li>推送机制：变化事件 → MQ/Feign 推送上游（骨架先提供查询接口，推送留 TODO）</li>
 *   <li>乐观锁：并发预占防超卖（PO 加 @Version）</li>
 * </ul>
 */
@Service
public class InventoryApplicationService {

    private final InventoryItemRepository inventoryItemRepository;

    public InventoryApplicationService(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    /**
     * 库存查询（供订单服务下单校验可售量）。
     */
    @Transactional(readOnly = true)
    public InventoryItem getBySkuCode(String skuCode) {
        return inventoryItemRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> new BizException("400001", "物料不存在: " + skuCode));
    }

    /**
     * 预占库存（下单时调用）。
     */
    @Transactional
    public InventoryItem reserve(String skuCode, java.math.BigDecimal quantity) {
        InventoryItem item = getBySkuCode(skuCode);
        item.reserve(quantity);
        inventoryItemRepository.save(item);
        // TODO（学习任务）：发布库存预占事件（对账/审计订阅）
        return item;
    }

    /**
     * 释放预占（取消/签收时调用）。
     */
    @Transactional
    public InventoryItem release(String skuCode, java.math.BigDecimal quantity) {
        InventoryItem item = getBySkuCode(skuCode);
        item.release(quantity);
        inventoryItemRepository.save(item);
        // TODO（学习任务）：发布库存释放事件
        return item;
    }

    /**
     * 物料入库/库存更新（主数据维护入口，也可由外部同步）。
     */
    @Transactional
    public InventoryItem upsert(String skuCode, String skuName, String unit,
                                java.math.BigDecimal totalQuantity) {
        InventoryItem item = inventoryItemRepository.findBySkuCode(skuCode).orElse(null);
        if (item == null) {
            item = new InventoryItem(
                    new com.demetrius.tribunal.inventory.domain.model.InventoryItemId(generateId()),
                    skuCode, skuName, unit, totalQuantity, java.math.BigDecimal.ZERO);
        }
        inventoryItemRepository.save(item);
        // TODO（学习任务）：物料/库存变化 → 推送上游（F-501 数据来源）
        return item;
    }

    private String generateId() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
}
