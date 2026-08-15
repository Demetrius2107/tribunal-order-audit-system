package com.demetrius.tribunal.order.application.dto;

import com.demetrius.tribunal.order.domain.model.PreOrderRecord;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预购订单记录应用层出参（F-312：保证金/补缴占用查询）。
 */
public record PreOrderRecordResult(
        String id,
        String activityNo,
        String orderNo,
        BigDecimal totalAmount,
        BigDecimal depositAmount,
        BigDecimal supplementAmount,
        LocalDateTime createTime) {

    /** 聚合 → 应用层 DTO。 */
    public static PreOrderRecordResult from(PreOrderRecord record) {
        return new PreOrderRecordResult(
                record.getId(),
                record.getActivityNo(),
                record.getOrderNo(),
                record.getTotalAmount(),
                record.getDepositAmount(),
                record.getSupplementAmount(),
                record.getCreateTime());
    }
}
