package com.demetrius.tribunal.inventory.application.dto;

import com.demetrius.tribunal.inventory.infrastructure.model.InventoryFlowPo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存变动流水项（对外报表：流水查询出参）。
 */
public record InventoryFlowResult(
        String id,
        String skuCode,
        String changeType,
        BigDecimal quantity,
        String sourceNo,
        LocalDateTime createTime) {

    public static InventoryFlowResult from(InventoryFlowPo po) {
        return new InventoryFlowResult(
                po.getId(),
                po.getSkuCode(),
                po.getChangeType(),
                po.getQuantity(),
                po.getSourceNo(),
                po.getCreateTime());
    }
}
