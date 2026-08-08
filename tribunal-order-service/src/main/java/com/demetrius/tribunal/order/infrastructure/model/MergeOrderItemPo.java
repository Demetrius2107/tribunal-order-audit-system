package com.demetrius.tribunal.order.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 合单明细持久化对象（对应 t_merge_order_item 表）。
 *
 * <p>每条明细记录来源订单 + 一个 SKU，合单创建时从成员订单的 OrderSku 展开。</p>
 */
@Data
@TableName("t_merge_order_item")
public class MergeOrderItemPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 合单 ID */
    private String mergeOrderId;

    /** 来源订单 ID */
    private String orderId;

    /** 来源订单编号 */
    private String orderNo;

    private String skuCode;

    private String skuName;

    /** 数量 */
    private BigDecimal quantity;

    /** 单价 */
    private BigDecimal unitAmount;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
