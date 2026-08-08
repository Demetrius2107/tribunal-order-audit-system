package com.demetrius.tribunal.inventorypush.application.service;

import com.demetrius.tribunal.common.dto.inventory.InventoryReceiveRequest;
import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.inventorypush.domain.model.IdempotentRecord;
import com.demetrius.tribunal.inventorypush.domain.model.InventoryLog;
import com.demetrius.tribunal.inventorypush.domain.model.InventorySku;
import com.demetrius.tribunal.inventorypush.domain.repository.IdempotentRecordRepository;
import com.demetrius.tribunal.inventorypush.domain.repository.InventoryLogRepository;
import com.demetrius.tribunal.inventorypush.domain.repository.InventorySkuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 库存推送接收应用服务（PRD 2.1 数据接收层 / 2.2 清洗转换 / 2.5 幂等）。
 *
 * <p>处理链路：鉴权 → 幂等检查 → 数据清洗 → 库存更新 → 流水记录 → 下游分发（PRD 6.1）。</p>
 *
 * <p>基建说明：当前实现完成幂等检查与库存落库主链路骨架，鉴权（HMAC-SHA256）、
 * 单位换算、SKU 编码映射、下游分发（MQ/回调）等留待后续按 PRD 填充。</p>
 */
@Service
public class InventoryReceiveApplicationService {

    private static final String IDEMPOTENT_SUCCESS = "SUCCESS";
    private static final String IDEMPOTENT_DAYS = "7";

    private final InventorySkuRepository inventorySkuRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final IdempotentRecordRepository idempotentRecordRepository;

    public InventoryReceiveApplicationService(InventorySkuRepository inventorySkuRepository,
                                              InventoryLogRepository inventoryLogRepository,
                                              IdempotentRecordRepository idempotentRecordRepository) {
        this.inventorySkuRepository = inventorySkuRepository;
        this.inventoryLogRepository = inventoryLogRepository;
        this.idempotentRecordRepository = idempotentRecordRepository;
    }

    /**
     * 接收上游库存推送（幂等：重复推送直接返回成功，PRD 2.5.1 FR-045/046）。
     */
    @Transactional
    public void receive(InventoryReceiveRequest request) {
        for (InventoryReceiveRequest.Item item : request.getItems()) {
            String idempotencyKey = buildIdempotencyKey(request, item);
            if (idempotentRecordRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
                // 幂等重复：已处理过，跳过（FR-046 记录重复日志，此处略）
                continue;
            }
            processItem(request.getBatchId(), item);
            saveIdempotentRecord(idempotencyKey, request.getBatchId());
        }
    }

    private void processItem(String batchId, InventoryReceiveRequest.Item item) {
        // 校验：可用库存不能为负（PRD 2.2.2 FR-018 异常拦截，此处抛错由全局异常处理）
        if (item.getInventory().getAvailableQty() != null && item.getInventory().getAvailableQty() < 0) {
            throw new BizException("INV-006", "可用库存不能为负: " + item.getSourceSkuId());
        }

        InventorySku sku = inventorySkuRepository
                .findBySkuWarehouseOwner(item.getSourceSkuId(), item.getWarehouseId(), item.getOwnerId())
                .orElseGet(() -> new InventorySku(
                        UUID.randomUUID().toString().replace("-", ""),
                        item.getSourceSkuId(), item.getWarehouseId(), item.getOwnerId(),
                        0, 0, 0, 0, 0, 0L));

        int beforeQty = sku.getAvailableQty();
        sku.applySnapshot(
                item.getInventory().getTotalQty() == null ? 0 : item.getInventory().getTotalQty(),
                item.getInventory().getAvailableQty() == null ? 0 : item.getInventory().getAvailableQty(),
                item.getInventory().getLockedQty() == null ? 0 : item.getInventory().getLockedQty(),
                item.getInventory().getInTransitQty() == null ? 0 : item.getInventory().getInTransitQty(),
                item.getInventory().getReservedQty() == null ? 0 : item.getInventory().getReservedQty(),
                item.getVersion() == null ? 0 : item.getVersion());

        inventorySkuRepository.save(sku);

        // 流水记录（PRD 2.3.1 FR-025）
        InventoryLog log = new InventoryLog(
                UUID.randomUUID().toString().replace("-", ""),
                item.getSourceSkuId(), item.getWarehouseId(), item.getOwnerId(),
                "PUSH", sku.getAvailableQty() - beforeQty, beforeQty, sku.getAvailableQty(),
                item.getBatchInfo() == null ? null : item.getBatchInfo().getBatchNo(),
                batchId, null);
        inventoryLogRepository.save(log);
    }

    private void saveIdempotentRecord(String idempotencyKey, String batchId) {
        String expireAt = LocalDateTime.now().plusDays(Long.parseLong(IDEMPOTENT_DAYS))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        idempotentRecordRepository.save(
                new IdempotentRecord(idempotencyKey, batchId, IDEMPOTENT_SUCCESS, expireAt));
    }

    /** 幂等键：batchId_skuId_warehouseId_version（PRD 2.5.1 FR-045） */
    private String buildIdempotencyKey(InventoryReceiveRequest request, InventoryReceiveRequest.Item item) {
        return request.getBatchId() + "_" + item.getSourceSkuId() + "_" + item.getWarehouseId() + "_" + item.getVersion();
    }
}
