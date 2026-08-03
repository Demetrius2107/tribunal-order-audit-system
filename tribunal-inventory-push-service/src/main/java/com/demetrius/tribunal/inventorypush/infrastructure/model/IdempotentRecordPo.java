package com.demetrius.tribunal.inventorypush.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 幂等记录持久化对象（对应 idempotent_record 表，PRD 5.1，幂等键为主键）。
 */
@Data
@TableName("idempotent_record")
public class IdempotentRecordPo {

    /** 幂等键：batchId_skuId_warehouseId_version */
    @TableId(value = "idempotency_key", type = IdType.INPUT)
    private String idempotencyKey;

    /** 上游推送批次号 */
    private String batchId;

    /** 状态：SUCCESS/FAILED */
    private String status;

    /** 过期时间 */
    private String expireAt;
}
