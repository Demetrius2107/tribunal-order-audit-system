package com.demetrius.tribunal.marketing.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板持久化对象（对应 t_coupon_template 表）。
 */
@Data
@TableName("t_coupon_template")
public class CouponTemplatePo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String templateNo;
    private String name;
    /** CouponType 枚举名 */
    private String type;

    private BigDecimal threshold;
    private BigDecimal deductionAmount;
    private BigDecimal discountRate;

    /** 总发放量（null = 不限） */
    private Integer totalQuota;
    /** 每人限领数量 */
    private Integer perUserLimit;
    /** 已发放数量 */
    private Integer issuedCount;

    private LocalDateTime validStartTime;
    private LocalDateTime validEndTime;

    private Boolean active;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
