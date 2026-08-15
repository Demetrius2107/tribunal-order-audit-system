package com.demetrius.tribunal.inventory.application.dto;

import java.util.List;

/**
 * 库存变动流水分页查询出参（对外报表）。
 */
public record InventoryFlowPage(
        long total,
        long pageNum,
        long pageSize,
        List<InventoryFlowResult> flows) {

    public static InventoryFlowPage of(long total, long pageNum, long pageSize,
                                       List<InventoryFlowResult> flows) {
        return new InventoryFlowPage(total, pageNum, pageSize, flows);
    }
}
