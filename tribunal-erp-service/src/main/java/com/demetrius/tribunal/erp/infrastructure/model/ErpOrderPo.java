package com.demetrius.tribunal.erp.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 履约订单持久化对象（对应 t_erp_order 表）。
 */
@Data
@TableName("t_erp_order")
public class ErpOrderPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 上游 OMS 订单编号（关联键，转单幂等依据） */
    private String sourceOrderNo;

    private String customerId;

    /** 状态（与 ErpOrderStatus.name() 对应） */
    private String status;

    private BigDecimal totalAmount;

    private LocalDateTime receivedAt;

    private LocalDateTime shippedAt;

    private LocalDateTime signedAt;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
