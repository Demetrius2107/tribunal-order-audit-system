package com.demetrius.tribunal.billing.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 金融账单持久化对象（对应 t_bill 表）。
 */
@Data
@TableName("t_bill")
public class BillPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 上游 订单服务 订单编号（关联键，转单幂等依据） */
    private String sourceOrderNo;

    private String customerId;

    /** 状态（与 BillStatus.name() 对应） */
    private String status;

    private BigDecimal totalAmount;

    private LocalDateTime generatedAt;

    private LocalDateTime confirmedAt;

    private LocalDateTime settledAt;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
