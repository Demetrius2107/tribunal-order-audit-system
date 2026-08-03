package com.demetrius.tribunal.fulfillment.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 履约单明细持久化对象（对应 t_fulfillment_line 表）。
 */
@Data
@TableName("t_fulfillment_line")
public class FulfillmentLinePo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String fulfillmentId;

    private String skuCode;

    private String skuName;

    private BigDecimal quantity;

    private BigDecimal price;

    private BigDecimal amount;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
