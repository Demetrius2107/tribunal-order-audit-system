package com.demetrius.tribunal.order.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预购订单记录（F-312：预购单参与活动的保证金/补缴占用）。
 *
 * <p>记录预购订单与活动的关联：商品总额、保证金金额（总额×保证金比例）、
 * 补缴金额（总额-保证金）。订单关闭时删除对应记录（业务文档七节）。</p>
 */
public class PreOrderRecord {

    private final String id;

    /** 预购活动编号 */
    private final String activityNo;

    /** 预购订单编号 */
    private final String orderNo;

    /** 预购应付总额（按预购专享折扣率计价后） */
    private final BigDecimal totalAmount;

    /** 保证金金额（总额×保证金比例） */
    private final BigDecimal depositAmount;

    /** 补缴金额（总额-保证金） */
    private final BigDecimal supplementAmount;

    private final LocalDateTime createTime;

    public PreOrderRecord(String id, String activityNo, String orderNo,
                          BigDecimal totalAmount, BigDecimal depositAmount,
                          BigDecimal supplementAmount, LocalDateTime createTime) {
        if (activityNo == null || activityNo.isBlank()) {
            throw new IllegalArgumentException("预购活动编号不能为空");
        }
        if (orderNo == null || orderNo.isBlank()) {
            throw new IllegalArgumentException("预购订单编号不能为空");
        }
        this.id = id;
        this.activityNo = activityNo;
        this.orderNo = orderNo;
        this.totalAmount = totalAmount;
        this.depositAmount = depositAmount;
        this.supplementAmount = supplementAmount;
        this.createTime = createTime;
    }

    // ---------- getters ----------

    public String getId() {
        return id;
    }

    public String getActivityNo() {
        return activityNo;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public BigDecimal getSupplementAmount() {
        return supplementAmount;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }
}
