package com.demetrius.tribunal.order.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 空包装回收明细持久化对象（对应 t_order_returnable 表，业务文档九节）。
 */
@Data
@TableName("t_order_returnable")
public class ReturnablePackagingPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String orderId;

    /** 包装类型编码 */
    private String packagingType;

    /** 包装类型名称 */
    private String packagingName;

    /** 回收数量 */
    private BigDecimal quantity;

    /** 单个包装押金 */
    private BigDecimal unitDeposit;

    /** 押金合计 = 数量 × 单价押金 */
    private BigDecimal depositAmount;

    @TableLogic
    private Integer deleted;
}
