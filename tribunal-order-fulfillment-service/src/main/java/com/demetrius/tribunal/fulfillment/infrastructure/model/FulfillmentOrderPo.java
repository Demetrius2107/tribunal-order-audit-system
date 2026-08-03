package com.demetrius.tribunal.fulfillment.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 履约单持久化对象（对应 t_fulfillment_order 表）。
 */
@Data
@TableName("t_fulfillment_order")
public class FulfillmentOrderPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String sourceOrderNo;

    private String customerId;

    /** 状态（与 FulfillmentStatus.name() 对应） */
    private String status;

    private BigDecimal totalAmount;

    private LocalDateTime createdAt;

    private LocalDateTime shippedAt;

    private LocalDateTime signedAt;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
