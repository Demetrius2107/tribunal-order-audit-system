package com.demetrius.tribunal.marketing.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 押金规则持久化对象（对应 t_deposit_rule 表）。
 */
@Data
@TableName("t_deposit_rule")
public class DepositRulePo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String skuCode;
    /** PackagingType 枚举名 */
    private String packagingType;
    private BigDecimal unitDeposit;
    /** 押金是否已含在售价中（true=不再额外加收） */
    private Boolean includedInPrice;
    private Boolean active;

    @TableLogic
    private Integer deleted;
}
