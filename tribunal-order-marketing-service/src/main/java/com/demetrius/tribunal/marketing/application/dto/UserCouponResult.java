package com.demetrius.tribunal.marketing.application.dto;

import com.demetrius.tribunal.marketing.domain.model.UserCoupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户券出参。
 */
public record UserCouponResult(
        String id,
        String couponCode,
        String templateId,
        String templateNo,
        String customerId,
        String type,
        BigDecimal threshold,
        BigDecimal deductionAmount,
        BigDecimal discountRate,
        String status,
        String orderId,
        LocalDateTime receiveTime,
        LocalDateTime validStartTime,
        LocalDateTime validEndTime,
        LocalDateTime usedTime,
        boolean valid) {

    public static UserCouponResult from(UserCoupon c) {
        return new UserCouponResult(
                c.getId(), c.getCouponCode(), c.getTemplateId(), c.getTemplateNo(),
                c.getCustomerId(), c.getType().name(),
                c.getThreshold(), c.getDeductionAmount(), c.getDiscountRate(),
                c.getStatus().name(), c.getOrderId(),
                c.getReceiveTime(), c.getValidStartTime(), c.getValidEndTime(),
                c.getUsedTime(),
                c.isValid(LocalDateTime.now()));
    }
}
