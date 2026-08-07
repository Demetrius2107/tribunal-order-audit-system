package com.demetrius.tribunal.common.dto.inventory;

import lombok.Data;

import java.util.List;

/**
 * 下游库存查询结果（对应 PRD 4.3 GET /api/v1/inventory/query）。
 */
@Data
public class InventorySkuView {

    /** 标准化 SKU 编码 */
    private String skuId;

    /** 仓库编码 */
    private String warehouseId;

    /** 可用库存 */
    private Integer availableQty;

    /** 总库存 */
    private Integer totalQty;

    /** 批次明细（按批次管理的库存） */
    private List<BatchView> batches;

    /**
     * 批次级库存视图。
     */
    @Data
    public static class BatchView {

        private String batchNo;

        private Integer availableQty;

        private String expiryDate;
    }
}
