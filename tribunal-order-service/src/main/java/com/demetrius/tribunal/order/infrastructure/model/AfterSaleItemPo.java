package com.demetrius.tribunal.order.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 售后明细持久化对象（对应 t_after_sale_item 表）。
 */
@Data
@TableName("t_after_sale_item")
public class AfterSaleItemPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 售后单 ID */
    private String afterSaleId;

    private String skuCode;

    private String skuName;

    /** 退货数量 */
    private BigDecimal quantity;

    /** 商品退款金额 */
    private BigDecimal refundAmount;

    /** 押金退还金额 */
    private BigDecimal depositRefund;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
