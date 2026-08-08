package com.demetrius.tribunal.marketing.domain.model;

/**
 * 促销计算上下文（调用方提供，用于规则匹配）。
 *
 * @param customerCode    客户编码（匹配 CUSTOMER 型规则）
 * @param customerGroupId 客户组编码（匹配 CUSTOMER_GROUP 型规则）
 */
public record PromotionContext(String customerCode, String customerGroupId) {
}
