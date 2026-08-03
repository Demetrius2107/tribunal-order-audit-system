package com.demetrius.tribunal.financesettlement.domain.model;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * 账户流水实体（对应 account_transaction 表，PRD 5.1）。
 *
 * <p>交易类型：SPLIT_IN/WITHDRAW_OUT/REFUND_OUT/FREEZE/UNFREEZE，支撑资金审计（FR-069）。</p>
 */
@Getter
public class AccountTransaction {

    private final String id;

    /** 流水号 */
    private final String transactionId;

    private final String accountId;

    /** 交易类型 */
    private final String transactionType;

    /** 金额 */
    private final BigDecimal amount;

    private final String relatedSettlementId;

    private final String relatedOrderId;

    /** 变动后余额 */
    private final BigDecimal balanceAfter;

    private final String description;

    public AccountTransaction(String id, String transactionId, String accountId, String transactionType,
                              BigDecimal amount, String relatedSettlementId, String relatedOrderId,
                              BigDecimal balanceAfter, String description) {
        this.id = id;
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.relatedSettlementId = relatedSettlementId;
        this.relatedOrderId = relatedOrderId;
        this.balanceAfter = balanceAfter;
        this.description = description;
    }
}
