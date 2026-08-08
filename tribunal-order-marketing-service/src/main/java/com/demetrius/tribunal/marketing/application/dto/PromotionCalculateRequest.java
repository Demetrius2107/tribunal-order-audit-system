package com.demetrius.tribunal.marketing.application.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 促销/押金计算请求（order-service → marketing-service）。
 *
 * @param customerCode     客户编码
 * @param customerGroupId  客户组编码
 * @param skuCodes         SKU 编码列表（用于查押金规则）
 * @param items            SKU 明细
 */
public record PromotionCalculateRequest(
        String customerCode,
        String customerGroupId,
        List<String> skuCodes,
        List<SkuItemDto> items) {

    /**
     * 单个 SKU 明细。
     *
     * @param skuCode  SKU 编码
     * @param skuName  SKU 名称
     * @param quantity 数量
     * @param price    单价
     */
    public record SkuItemDto(String skuCode, String skuName,
                             BigDecimal quantity, BigDecimal price) {
    }
}
