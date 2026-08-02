package com.demetrius.tribunal.marketing.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 价格规则持久化对象（对应 t_price_rule 表）。
 */
@Data
@TableName("t_price_rule")
public class PriceRulePo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String skuCode;

    /** 价格档位：CUSTOMER / CUSTOMER_GROUP / AREA */
    private String priceLevel;

    /** 价格对象编码（客户编码/客户组编码/区域编码） */
    private String priceTarget;

    private BigDecimal price;

    private String currency;

    @TableLogic
    private Integer deleted;
}
