package com.demetrius.tribunal.inventorypush.domain.model;

import lombok.Getter;

/**
 * 批次库存领域实体（对应 inventory_batch 表，PRD 5.1）。
 *
 * <p>承载生产日期/有效期，支撑 FIFO/FEFO 与临期预警（PRD 2.3.3）。</p>
 */
@Getter
public class InventoryBatch {

    private final String id;

    private final String skuId;

    private final String warehouseId;

    private final String batchNo;

    private final String productionDate;

    private final String expiryDate;

    private int qty;

    /** 状态：VALID/EXPIRED/FROZEN */
    private String status;

    public InventoryBatch(String id, String skuId, String warehouseId, String batchNo,
                          String productionDate, String expiryDate, int qty, String status) {
        this.id = id;
        this.skuId = skuId;
        this.warehouseId = warehouseId;
        this.batchNo = batchNo;
        this.productionDate = productionDate;
        this.expiryDate = expiryDate;
        this.qty = qty;
        this.status = status;
    }
}
