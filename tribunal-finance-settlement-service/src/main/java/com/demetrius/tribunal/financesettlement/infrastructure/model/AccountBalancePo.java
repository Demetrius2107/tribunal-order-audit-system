package com.demetrius.tribunal.financesettlement.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 分账方账户余额表持久化对象（对应 account_balance 表，PRD 5.1）。
 */
@Data
@TableName("account_balance")
public class AccountBalancePo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 账户 ID */
    private String accountId;

    /** 所属方 ID */
    private String ownerId;

    /** 所属方类型 */
    private String ownerType;

    /** 可用余额 */
    private BigDecimal availableBalance;

    /** 冻结余额 */
    private BigDecimal frozenBalance;

    /** 在途金额 */
    private BigDecimal inTransitAmount;

    private String currency;

    /** 乐观锁版本号 */
    private Long version;
}
