package com.demetrius.tribunal.billing.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 金融账单明细持久化对象（对应 t_bill_line 表）。
 */
@Data
@TableName("t_bill_line")
public class BillLinePo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String billId;

    private String skuCode;

    private String skuName;

    private BigDecimal quantity;

    private BigDecimal price;

    private BigDecimal amount;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
