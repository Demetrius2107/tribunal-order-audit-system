package com.demetrius.tribunal.order.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细持久化对象（对应 t_order_sku 表）。
 */
@Data
@TableName("t_order_sku")
public class OrderSkuPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String orderId;

    private String skuCode;

    private String skuName;

    private BigDecimal quantity;

    private BigDecimal price;

    private BigDecimal amount;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
