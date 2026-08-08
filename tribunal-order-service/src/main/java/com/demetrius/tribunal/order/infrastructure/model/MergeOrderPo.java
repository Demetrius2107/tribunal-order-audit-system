package com.demetrius.tribunal.order.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 合单持久化对象（对应 t_merge_order 表）。
 */
@Data
@TableName("t_merge_order")
public class MergeOrderPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 合单编号（业务唯一键） */
    private String mergeNo;

    /** 合单客户（所有成员订单共享） */
    private String customerId;

    /** 状态（与 MergeOrderStatus.name() 对应） */
    private String status;

    /** 合单运费 */
    private BigDecimal shippingFee;

    /** 物流单号 */
    private String trackingNo;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
