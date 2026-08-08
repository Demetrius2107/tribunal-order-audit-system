package com.demetrius.tribunal.order.client;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 促销计算结果（marketing-service → order-service）。
 */
public record PromotionCalculateResponse(
        BigDecimal discountAmount,
        BigDecimal depositAmount,
        BigDecimal payableAddition,
        List<String> appliedPromotionIds,
        List<GiftItemDto> giftItems,
        Map<String, BigDecimal> discountBreakdown,
        Map<String, BigDecimal> depositBreakdown) {

    public record GiftItemDto(String skuCode, String skuName, BigDecimal quantity) {
    }
}
