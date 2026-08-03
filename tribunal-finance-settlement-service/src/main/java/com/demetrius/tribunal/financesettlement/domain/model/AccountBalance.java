package com.demetrius.tribunal.financesettlement.domain.model;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * 分账方虚拟账户实体（对应 account_balance 表，PRD 5.1）。
 *
 * <p>记录可用/冻结/在途余额，乐观锁版本号防并发透支（FR-032/FR-035）。</p>
 */
@Getter
public class AccountBalance {

    private final String id;

    /** 账户 ID */
    private final String accountId;

    /** 所属方 ID（商户/物流/平台） */
    private final String ownerId;

    /** 所属方类型 */
    private final String ownerType;

    /** 可用余额 */
    private BigDecimal availableBalance;

    /** 冻结余额 */
    private BigDecimal frozenBalance;

    /** 在途金额 */
    private BigDecimal inTransitAmount;

    private final String currency;

    /** 乐观锁版本号 */
    private long version;

    public AccountBalance(String id, String accountId, String ownerId, String ownerType,
                          BigDecimal availableBalance, BigDecimal frozenBalance,
                          BigDecimal inTransitAmount, String currency, long version) {
        this.id = id;
        this.accountId = accountId;
        this.ownerId = ownerId;
        this.ownerType = ownerType;
        this.availableBalance = availableBalance;
        this.frozenBalance = frozenBalance;
        this.inTransitAmount = inTransitAmount;
        this.currency = currency;
        this.version = version;
    }

    /** 入账（分账转入） */
    public void credit(BigDecimal amount) {
        this.availableBalance = this.availableBalance.add(amount);
        this.version++;
    }

    /** 出账（分账回退/提现），余额不足抛异常由应用层处理 */
    public void debit(BigDecimal amount) {
        this.availableBalance = this.availableBalance.subtract(amount);
        this.version++;
    }
}
