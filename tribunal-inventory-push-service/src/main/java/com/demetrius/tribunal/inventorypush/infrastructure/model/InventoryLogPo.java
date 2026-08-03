package com.demetrius.tribunal.inventorypush.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 库存流水持久化对象（对应 inventory_log 表，PRD 5.1）。
 */
@Data
@TableName("inventory_log")
public class InventoryLogPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String skuId;

    private String warehouseId;

    private String ownerId;

    /** 变动类型：PUSH/LOCK/UNLOCK/RESERVE */
    private String changeType;

    /** 变动数量（可正可负） */
    private Integer deltaQty;

    private Integer beforeQty;

    private Integer afterQty;

    /** 关联批次号 */
    private String batchId;

    /** 上游推送批次号 */
    private String sourceBatchId;

    /** 下游分发消息 ID */
    private String messageId;
}
