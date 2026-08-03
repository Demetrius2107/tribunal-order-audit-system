package com.demetrius.tribunal.inventorypush.common.dto;

import lombok.Data;

/**
 * 批次信息（对应 PRD 2.3.3 效期与批次管理：生产日期/有效期/批次号）。
 */
@Data
public class InventoryBatchInfo {

    /** 批次号 */
    private String batchNo;

    /** 生产日期（ISO-8601） */
    private String productionDate;

    /** 有效期至（ISO-8601） */
    private String expiryDate;
}
