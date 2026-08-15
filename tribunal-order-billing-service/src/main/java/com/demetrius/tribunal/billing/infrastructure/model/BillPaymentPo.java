package com.demetrius.tribunal.billing.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收款流水持久化对象（对应 t_bill_payment 表，F-606：审计 + 对账）。
 */
@Data
@TableName("t_bill_payment")
public class BillPaymentPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 账单ID */
    private String billId;

    /** 上游订单编号 */
    private String sourceOrderNo;

    /** 收款金额 */
    private BigDecimal amount;

    /** 收款时间 */
    private LocalDateTime paymentTime;

    /** 操作人 */
    private String operator;
}
