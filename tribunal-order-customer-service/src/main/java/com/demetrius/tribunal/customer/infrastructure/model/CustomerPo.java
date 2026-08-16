package com.demetrius.tribunal.customer.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 客户持久化对象（对应 t_customer 表）。
 */
@Data
@TableName("t_customer")
public class CustomerPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String customerCode;

    private String name;

    /** 信用总额度 */
    private BigDecimal creditLimit;

    /** 已占用信用 */
    private BigDecimal creditUsed;

    /** 折扣池余额（促销返还，可抵扣应付，F-203） */
    private BigDecimal discountPoolBalance;

    @TableLogic
    private Integer deleted;
}
