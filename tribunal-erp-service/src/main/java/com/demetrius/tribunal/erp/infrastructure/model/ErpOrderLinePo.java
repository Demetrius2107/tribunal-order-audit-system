package com.demetrius.tribunal.erp.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 履约订单明细持久化对象（对应 t_erp_order_line 表）。
 */
@Data
@TableName("t_erp_order_line")
public class ErpOrderLinePo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String erpOrderId;

    private String skuCode;

    private String skuName;

    private BigDecimal quantity;

    private BigDecimal price;

    private BigDecimal amount;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
