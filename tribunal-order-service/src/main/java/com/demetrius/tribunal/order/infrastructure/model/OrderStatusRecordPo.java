package com.demetrius.tribunal.order.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单状态流水持久化对象（对应 t_order_status_record 表）。
 *
 * <p>每次订单状态迁移写一条流水 = 审计 + 幂等判断依据
 * （参照通用做法：订单状态流水表）。</p>
 */
@Data
@TableName("t_order_status_record")
public class OrderStatusRecordPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String orderId;

    private String fromStatus;

    private String toStatus;

    private String operator;

    private LocalDateTime createTime;
}
