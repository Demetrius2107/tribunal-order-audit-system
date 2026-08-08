package com.demetrius.tribunal.marketing.domain.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 促销计算结果。
 *
 * @param discountAmount      折扣总金额（已叠加所有可叠加规则）
 * @param appliedRuleIds      实际生效的规则 ID 列表（用于审计/对账）
 * @param giftItems           赠品清单（满赠产出）
 * @param skuDiscountBreakdown 折扣按 SKU 比例分摊明细（skuCode → 折扣额，支持退货退款）
 */
public record PromotionResult(BigDecimal discountAmount,
                              List<String> appliedRuleIds,
                              List<GiftItem> giftItems,
                              Map<String, BigDecimal> skuDiscountBreakdown) {

    /** 空结果（无任何促销命中） */
    public static PromotionResult empty() {
        return new PromotionResult(BigDecimal.ZERO, List.of(), List.of(), Collections.emptyMap());
    }

    /** 合并两条促销结果（用于规则叠加） */
    public PromotionResult merge(PromotionResult other) {
        Map<String, BigDecimal> merged = new LinkedHashMap<>(this.skuDiscountBreakdown);
        other.skuDiscountBreakdown.forEach((k, v) ->
                merged.merge(k, v, BigDecimal::add));
        return new PromotionResult(
                this.discountAmount.add(other.discountAmount),
                concat(this.appliedRuleIds, other.appliedRuleIds),
                concat(this.giftItems, other.giftItems),
                Collections.unmodifiableMap(merged));
    }

    private static <T> List<T> concat(List<T> a, List<T> b) {
        var r = new java.util.ArrayList<>(a);
        r.addAll(b);
        return Collections.unmodifiableList(r);
    }
}
