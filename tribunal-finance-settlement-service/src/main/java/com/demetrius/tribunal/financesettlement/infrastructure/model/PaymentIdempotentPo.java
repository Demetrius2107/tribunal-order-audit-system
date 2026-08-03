package com.demetrius.tribunal.financesettlement.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 扣款幂等控制表持久化对象（对应 payment_idempotent 表，PRD 5.1，幂等键为主键）。
 */
@Data
@TableName("payment_idempotent")
public class PaymentIdempotentPo {

    /** 幂等键：settlementId_batchNo */
    @TableId(value = "idempotency_key", type = IdType.INPUT)
    private String idempotencyKey;

    private String settlementId;

    /** 状态：SUCCESS/FAILED/PROCESSING */
    private String status;

    /** 渠道原始响应 */
    private String channelResponse;

    /** 过期时间 */
    private String expireAt;
}
