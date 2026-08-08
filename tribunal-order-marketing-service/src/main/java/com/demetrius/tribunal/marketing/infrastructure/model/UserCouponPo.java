package com.demetrius.tribunal.marketing.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户券持久化对象（对应 t_user_coupon 表）。
 */
@Data
@TableName("t_user_coupon")
public class UserCouponPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String couponCode;
    private String templateId;
    private String templateNo;
    private String customerId;

    /** CouponType 枚举名（从模板冗余） */
    private String type;
    private BigDecimal threshold;
    private BigDecimal deductionAmount;
    private BigDecimal discountRate;

    /** UserCouponStatus 枚举名 */
    private String status;
    /** 使用/锁定时关联的订单 ID */
    private String orderId;

    private LocalDateTime receiveTime;
    private LocalDateTime validStartTime;
    private LocalDateTime validEndTime;
    private LocalDateTime usedTime;

    /** 乐观锁版本号（防并发重复核销） */
    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;
}
