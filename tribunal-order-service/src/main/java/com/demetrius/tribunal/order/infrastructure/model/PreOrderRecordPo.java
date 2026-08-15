package com.demetrius.tribunal.order.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预购订单记录持久化对象（对应 t_pre_order_record 表，F-312）。
 */
@Data
@TableName("t_pre_order_record")
public class PreOrderRecordPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 预购活动编号 */
    private String activityNo;

    /** 预购订单编号 */
    private String orderNo;

    /** 预购应付总额 */
    private BigDecimal totalAmount;

    /** 保证金金额 */
    private BigDecimal depositAmount;

    /** 补缴金额 */
    private BigDecimal supplementAmount;

    private LocalDateTime createTime;
}
