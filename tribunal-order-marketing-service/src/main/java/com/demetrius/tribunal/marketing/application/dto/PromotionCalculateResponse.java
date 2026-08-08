package com.demetrius.tribunal.marketing.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 促销计算结果（marketing-service → order-service）。
 *
 * @param discountAmount      折扣总金额
 * @param depositAmount       押金总金额（额外加收部分）
 * @param payableAddition     应付金额增量 = discountAmount(neg) + depositAmount(pos)
 * @param appliedPromotionIds 生效的促销规则 ID
 * @param giftItems           赠品清单
 * @param discountBreakdown   折扣按 SKU 分摊（skuCode → 折扣额）
 * @param depositBreakdown    押金按 SKU 分摊（skuCode → 押金额）
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
