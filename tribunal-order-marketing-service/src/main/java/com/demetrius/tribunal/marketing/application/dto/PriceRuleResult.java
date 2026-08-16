package com.demetrius.tribunal.marketing.application.dto;

import com.demetrius.tribunal.marketing.domain.model.PriceRule;

import java.math.BigDecimal;

/**
 * 价格规则配置出参（F-102 价格体系配置化）。
 */
public record PriceRuleResult(
        String ruleId,
        String skuCode,
        String priceLevel,
        String priceTarget,
        BigDecimal price,
        String currency) {

    /** 聚合 → 应用层 DTO。 */
    public static PriceRuleResult from(PriceRule rule) {
        return new PriceRuleResult(
                rule.getId().value(),
                rule.getSkuCode(),
                rule.getPriceLevel(),
                rule.getPriceTarget(),
                rule.getPrice(),
                rule.getCurrency());
    }
}
