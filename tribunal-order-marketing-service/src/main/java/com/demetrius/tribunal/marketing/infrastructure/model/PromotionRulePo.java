package com.demetrius.tribunal.marketing.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 促销规则持久化对象（对应 t_promotion_rule 表）。
 */
@Data
@TableName("t_promotion_rule")
public class PromotionRulePo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String ruleNo;
    private String name;
    /** PromotionType 枚举名 */
    private String type;
    /** PromotionTargetType 枚举名 */
    private String targetType;
    private String targetValue;

    private BigDecimal threshold;
    private BigDecimal discountRate;
    private BigDecimal reductionAmount;
    private BigDecimal halfPriceRate;
    private String applicableSkuCode;

    private String giftSkuCode;
    private String giftSkuName;
    private BigDecimal giftQuantity;

    /** 0=可叠加，1=互斥（应用后终止后续规则） */
    private Boolean exclusive;
    private Integer priority;
    private Boolean active;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @TableLogic
    private Integer deleted;
}
