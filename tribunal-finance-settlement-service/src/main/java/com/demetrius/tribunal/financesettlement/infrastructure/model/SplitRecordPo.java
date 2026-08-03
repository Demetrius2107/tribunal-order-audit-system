package com.demetrius.tribunal.financesettlement.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 分账记录表持久化对象（对应 split_record 表，PRD 5.1）。
 */
@Data
@TableName("split_record")
public class SplitRecordPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String settlementId;

    /** 收款方 ID */
    private String recipientId;

    /** 收款方类型：MERCHANT/PLATFORM/LOGISTICS/AGENT */
    private String recipientType;

    /** 分账金额 */
    private BigDecimal splitAmount;

    /** 分账比例 */
    private BigDecimal splitRate;

    /** 状态：PENDING/SUCCESS/FAILED */
    private String status;

    private String channelTransactionId;
}
