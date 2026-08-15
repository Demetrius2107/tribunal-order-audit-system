package com.demetrius.tribunal.marketing.application.dto;

import com.demetrius.tribunal.marketing.domain.model.PromotionRule;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 促销规则配置出参（配置化接口：创建/上线/停用/查询）。
 */
public record PromotionRuleResult(
        String ruleId,
        String ruleNo,
        String name,
        String type,
        String targetType,
        String targetValue,
        BigDecimal threshold,
        BigDecimal discountRate,
        BigDecimal reductionAmount,
        BigDecimal halfPriceRate,
        String applicableSkuCode,
        String giftSkuCode,
        String giftSkuName,
        BigDecimal giftQuantity,
        boolean exclusive,
        int priority,
        boolean active,
        LocalDateTime startTime,
        LocalDateTime endTime) {

    /** 聚合 → 应用层 DTO。 */
    public static PromotionRuleResult from(PromotionRule rule) {
        return new PromotionRuleResult(
                rule.getId(),
                rule.getRuleNo(),
                rule.getName(),
                rule.getType().name(),
                rule.getTargetType().name(),
                rule.getTargetValue(),
                rule.getThreshold(),
                rule.getDiscountRate(),
                rule.getReductionAmount(),
                rule.getHalfPriceRate(),
                rule.getApplicableSkuCode(),
                rule.getGiftSkuCode(),
                rule.getGiftSkuName(),
                rule.getGiftQuantity(),
                rule.isExclusive(),
                rule.getPriority(),
                rule.isActive(),
                rule.getStartTime(),
                rule.getEndTime());
    }
}
