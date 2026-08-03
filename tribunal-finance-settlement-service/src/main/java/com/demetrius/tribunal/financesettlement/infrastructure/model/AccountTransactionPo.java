package com.demetrius.tribunal.financesettlement.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 账户流水表持久化对象（对应 account_transaction 表，PRD 5.1）。
 */
@Data
@TableName("account_transaction")
public class AccountTransactionPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 流水号 */
    private String transactionId;

    private String accountId;

    /** 交易类型：SPLIT_IN/WITHDRAW_OUT/REFUND_OUT/FREEZE/UNFREEZE */
    private String transactionType;

    private BigDecimal amount;

    private String relatedSettlementId;

    private String relatedOrderId;

    /** 变动后余额 */
    private BigDecimal balanceAfter;

    private String description;
}
