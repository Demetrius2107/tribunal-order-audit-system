package com.demetrius.tribunal.order.client;

import java.math.BigDecimal;
import java.util.List;

/**
 * 促销/押金计算请求（order-service → marketing-service）。
 */
public record PromotionCalculateRequest(
        String customerCode,
        String customerGroupId,
        List<String> skuCodes,
        List<SkuItemDto> items) {

    public record SkuItemDto(String skuCode, String skuName,
                             BigDecimal quantity, BigDecimal price) {
    }
}
