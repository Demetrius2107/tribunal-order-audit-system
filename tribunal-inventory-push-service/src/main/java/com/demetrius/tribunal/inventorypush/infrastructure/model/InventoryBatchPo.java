package com.demetrius.tribunal.inventorypush.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 批次库存持久化对象（对应 inventory_batch 表，PRD 5.1）。
 */
@Data
@TableName("inventory_batch")
public class InventoryBatchPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String skuId;

    private String warehouseId;

    private String batchNo;

    private String productionDate;

    private String expiryDate;

    private Integer qty;

    /** 状态：VALID/EXPIRED/FROZEN */
    private String status;
}
