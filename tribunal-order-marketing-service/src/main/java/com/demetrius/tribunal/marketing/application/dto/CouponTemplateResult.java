package com.demetrius.tribunal.marketing.application.dto;

import com.demetrius.tribunal.marketing.domain.model.CouponTemplate;
import com.demetrius.tribunal.marketing.domain.model.CouponType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 券模板出参。
 */
public record CouponTemplateResult(
        String id,
        String templateNo,
        String name,
        String type,
        BigDecimal threshold,
        BigDecimal deductionAmount,
        BigDecimal discountRate,
        Integer totalQuota,
        Integer perUserLimit,
        Integer issuedCount,
        Integer remaining,
        LocalDateTime validStartTime,
        LocalDateTime validEndTime,
        boolean active,
        boolean valid) {

    public static CouponTemplateResult from(CouponTemplate t) {
        int remaining = t.getTotalQuota() == null ? -1 : Math.max(0, t.getTotalQuota() - t.getIssuedCount());
        return new CouponTemplateResult(
                t.getId(), t.getTemplateNo(), t.getName(),
                t.getType().name(),
                t.getThreshold(), t.getDeductionAmount(), t.getDiscountRate(),
                t.getTotalQuota(), t.getPerUserLimit(), t.getIssuedCount(),
                remaining,
                t.getValidStartTime(), t.getValidEndTime(),
                t.isActive(), t.isValid(LocalDateTime.now()));
    }
}
