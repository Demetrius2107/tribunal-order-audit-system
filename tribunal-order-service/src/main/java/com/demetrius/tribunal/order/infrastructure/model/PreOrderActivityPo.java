package com.demetrius.tribunal.order.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预购活动主档持久化对象（对应 t_pre_order_activity 表，F-312）。
 */
@Data
@TableName("t_pre_order_activity")
public class PreOrderActivityPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 预购活动编号（业务唯一键） */
    private String activityNo;

    private String name;

    /** 参与 SKU 范围（逗号分隔，可为空 = 全部） */
    private String skuCodes;

    /** 保证金比例（如 0.3 = 30%） */
    private BigDecimal depositRate;

    /** 预购专享折扣率（如 0.9 = 9 折） */
    private BigDecimal discountRate;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 状态：DRAFT/ACTIVE/ENDED/CANCELLED（与 PreOrderActivityStatus.name() 对应） */
    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
