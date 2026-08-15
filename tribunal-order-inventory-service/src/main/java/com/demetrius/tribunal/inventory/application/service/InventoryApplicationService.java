package com.demetrius.tribunal.inventory.application.service;

import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.inventory.domain.model.InventoryItem;
import com.demetrius.tribunal.inventory.domain.repository.InventoryItemRepository;
import com.demetrius.tribunal.inventory.infrastructure.mapper.InventoryFlowMapper;
import com.demetrius.tribunal.inventory.infrastructure.model.InventoryFlowPo;
import com.demetrius.tribunal.inventory.infrastructure.repository.InventoryItemRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(InventoryApplicationService.class);

    /** 乐观锁冲突最大重试次数（读-改-写循环） */
    private static final int MAX_RETRY = 3;

    private final InventoryItemRepository inventoryItemRepository;

    private final InventoryFlowMapper inventoryFlowMapper;

    public InventoryApplicationService(InventoryItemRepository inventoryItemRepository,
                                       InventoryFlowMapper inventoryFlowMapper) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryFlowMapper = inventoryFlowMapper;
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
        InventoryItem item = mutateWithRetry(skuCode, quantity, InventoryItem::reserve);
        recordFlow(skuCode, "RESERVE", quantity);
        // TODO（学习任务）：发布库存预占事件（对账/审计订阅）
        return item;
    }

    /**
     * 释放预占（取消/签收时调用）。
     */
    @Transactional
    public InventoryItem release(String skuCode, java.math.BigDecimal quantity) {
        InventoryItem item = mutateWithRetry(skuCode, quantity, InventoryItem::release);
        recordFlow(skuCode, "RELEASE", quantity);
        // TODO（学习任务）：发布库存释放事件
        return item;
    }

    /**
     * 退货入库（售后退货完成时调用）。
     */
    @Transactional
    public InventoryItem returnStock(String skuCode, java.math.BigDecimal quantity) {
        InventoryItem item = mutateWithRetry(skuCode, quantity, InventoryItem::returnStock);
        recordFlow(skuCode, "IN", quantity);
        return item;
    }

    /**
     * 读-改-写 + 乐观锁冲突重试（并发超卖防护）。
     *
     * <p>并发预占同一 SKU 时，多个事务可能读到同一 version；写回时 {@code updateById}
     * 带 {@code WHERE version=?}，后写者影响行数为 0 → 仓储抛乐观锁冲突 →
     * 重新读取最新库存（含新 version）再试，最多 {@link #MAX_RETRY} 次。</p>
     */
    private InventoryItem mutateWithRetry(String skuCode, java.math.BigDecimal quantity,
                                          InventoryMutation mutation) {
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            InventoryItem item = getBySkuCode(skuCode);
            mutation.apply(item, quantity);
            try {
                inventoryItemRepository.save(item);
                return item;
            } catch (InventoryItemRepositoryImpl.OptimisticLockConflictException e) {
                if (attempt == MAX_RETRY) {
                    throw new BizException("400002",
                            "库存并发冲突，重试 " + MAX_RETRY + " 次仍失败: " + skuCode);
                }
                log.warn("库存乐观锁冲突，重读重试 attempt={} skuCode={}", attempt + 1, skuCode);
            }
        }
        throw new IllegalStateException("不可达：重试循环已处理所有分支");
    }

    /** 库存变更动作（领域方法引用）。 */
    @FunctionalInterface
    private interface InventoryMutation {
        void apply(InventoryItem item, java.math.BigDecimal quantity);
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

    /**
     * 记录库存变动流水（F-504：审计 + 对账）。
     *
     * @param changeType 变动类型（IN/OUT/RESERVE/RELEASE）
     */
    private void recordFlow(String skuCode, String changeType, java.math.BigDecimal quantity) {
        InventoryFlowPo flow = new InventoryFlowPo();
        flow.setSkuCode(skuCode);
        flow.setChangeType(changeType);
        flow.setQuantity(quantity);
        inventoryFlowMapper.insert(flow);
    }
}
