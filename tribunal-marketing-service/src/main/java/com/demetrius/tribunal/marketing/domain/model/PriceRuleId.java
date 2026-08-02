package com.demetrius.tribunal.marketing.domain.model;

/**
 * 价格规则 ID 值对象。
 */
public record PriceRuleId(String value) {

    public PriceRuleId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("价格规则ID不能为空");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
