package com.demetrius.tribunal.marketing.domain.service;

import com.demetrius.tribunal.marketing.domain.model.GiftItem;
import com.demetrius.tribunal.marketing.domain.model.PromotionContext;
import com.demetrius.tribunal.marketing.domain.model.PromotionResult;
import com.demetrius.tribunal.marketing.domain.model.PromotionRule;
import com.demetrius.tribunal.marketing.domain.model.PromotionType;
import com.demetrius.tribunal.marketing.domain.model.SkuItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 促销计算引擎（M4 领域服务，纯函数无副作用，便于单测）。
 *
 * <p>计算流程：</p>
 * <ol>
 *   <li>筛选：active + 有效期内 + 匹配客户上下文</li>
 *   <li>排序：按 {@link PromotionRule#getPriority()} 升序</li>
 *   <li>逐条应用：每条规则基于"剩余可折扣金额"计算（累扣防超扣）</li>
 *   <li>互斥短路：遇到 {@link PromotionRule#isExclusive()} = true 的规则，应用后终止</li>
 *   <li>金额分摊：每条折扣按 SKU 金额占比分摊到明细（末 SKU 吸收尾差，保证求和一致）</li>
 * </ol>
 */
public class PromotionEngine {

    private static final BigDecimal TWO = new BigDecimal("2");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    /**
     * 计算促销。
     *
     * @param skus    订单明细快照（不可变）
     * @param rules   候选促销规则
     * @param context 客户上下文（用于规则匹配）
     * @return 促销结果（折扣金额 + 赠品 + SKU 分摊明细）
     */
    public PromotionResult calculate(List<SkuItem> skus, List<PromotionRule> rules,
                                     PromotionContext context) {
        if (skus == null || skus.isEmpty() || rules == null || rules.isEmpty()) {
            return PromotionResult.empty();
        }

        LocalDateTime now = LocalDateTime.now();
        BigDecimal subtotal = skus.stream()
                .map(SkuItem::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return PromotionResult.empty();
        }

        // 筛选 + 排序
        List<PromotionRule> applicable = rules.stream()
                .filter(r -> r.isEffective(now) && r.matches(context))
                .sorted(Comparator.comparingInt(PromotionRule::getPriority))
                .toList();

        BigDecimal accumulatedDiscount = BigDecimal.ZERO;
        List<String> appliedIds = new ArrayList<>();
        List<GiftItem> gifts = new ArrayList<>();
        Map<String, BigDecimal> breakdown = new LinkedHashMap<>();

        for (PromotionRule rule : applicable) {
            BigDecimal remaining = subtotal.subtract(accumulatedDiscount);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal ruleDiscount = applyRule(rule, remaining, skus, breakdown, gifts);

            boolean hit = ruleDiscount.compareTo(BigDecimal.ZERO) > 0
                    || (rule.getType() == PromotionType.GIFT && !gifts.isEmpty());
            if (hit) {
                appliedIds.add(rule.getId());
                accumulatedDiscount = accumulatedDiscount.add(ruleDiscount);
            }
            if (rule.isExclusive()) {
                break;
            }
        }

        if (accumulatedDiscount.compareTo(BigDecimal.ZERO) == 0 && gifts.isEmpty()) {
            return PromotionResult.empty();
        }
        return new PromotionResult(accumulatedDiscount,
                List.copyOf(appliedIds), List.copyOf(gifts),
                breakdown.isEmpty() ? Map.of() : Map.copyOf(breakdown));
    }

    /**
     * 应用单条规则，返回该规则产生的折扣金额（已累加到 breakdown）。
     *
     * @param remaining 剩余可折扣金额（防止多条规则叠加后超过商品总额）
     */
    private BigDecimal applyRule(PromotionRule rule, BigDecimal remaining,
                                 List<SkuItem> skus,
                                 Map<String, BigDecimal> breakdown,
                                 List<GiftItem> gifts) {
        return switch (rule.getType()) {
            case FULL_REDUCTION -> {
                if (rule.getThreshold() != null && remaining.compareTo(rule.getThreshold()) >= 0) {
                    BigDecimal d = rule.getReductionAmount().min(remaining)
                            .setScale(2, RoundingMode.HALF_UP);
                    prorate(breakdown, skus, d);
                    yield d;
                }
                yield BigDecimal.ZERO;
            }
            case DISCOUNT -> {
                BigDecimal afterDiscount = remaining.multiply(rule.getDiscountRate())
                        .setScale(2, RoundingMode.HALF_UP);
                BigDecimal discount = remaining.subtract(afterDiscount);
                prorate(breakdown, skus, discount);
                yield discount;
            }
            case SECOND_HALF_PRICE -> calcSecondHalfPrice(rule, skus, breakdown);
            case GIFT -> {
                if (rule.getThreshold() == null || remaining.compareTo(rule.getThreshold()) >= 0) {
                    gifts.add(new GiftItem(rule.getGiftSkuCode(),
                            rule.getGiftSkuName(), rule.getGiftQuantity()));
                }
                yield BigDecimal.ZERO;
            }
        };
    }

    /**
     * 第二件半价：对每件适用的 SKU，每满 2 件第 2 件按 halfPriceRate 计价。
     *
     * <p>例：数量 5、单价 10、halfPriceRate=0.5 → 半价件数 = floor(5/2) = 2，
     * 折扣 = 2 × 10 × (1 - 0.5) = 10。</p>
     */
    private BigDecimal calcSecondHalfPrice(PromotionRule rule, List<SkuItem> skus,
                                           Map<String, BigDecimal> breakdown) {
        BigDecimal totalDiscount = BigDecimal.ZERO;
        Map<String, BigDecimal> perSku = new LinkedHashMap<>();
        for (SkuItem sku : skus) {
            if (!rule.appliesToSku(sku.skuCode())) {
                continue;
            }
            BigDecimal halfItems = sku.quantity().divide(TWO, 0, RoundingMode.DOWN);
            if (halfItems.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal discount = halfItems.multiply(sku.price())
                    .multiply(BigDecimal.ONE.subtract(rule.getHalfPriceRate()))
                    .setScale(2, RoundingMode.HALF_UP);
            totalDiscount = totalDiscount.add(discount);
            perSku.merge(sku.skuCode(), discount, BigDecimal::add);
        }
        // 第二件半价的分摊直接对应到具体 SKU，无需按比例
        perSku.forEach((k, v) -> breakdown.merge(k, v, BigDecimal::add));
        return totalDiscount;
    }

    /**
     * 将折扣按 SKU 金额占比分摊到明细，最后一个 SKU 吸收舍入尾差（保证求和 == discount）。
     */
    private void prorate(Map<String, BigDecimal> breakdown, List<SkuItem> skus,
                         BigDecimal discount) {
        if (discount.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        BigDecimal total = skus.stream().map(SkuItem::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < skus.size(); i++) {
            SkuItem sku = skus.get(i);
            BigDecimal share;
            if (i == skus.size() - 1) {
                // 末项吸收尾差
                share = discount.subtract(allocated);
            } else {
                share = discount.multiply(sku.amount())
                        .divide(total, 2, RoundingMode.HALF_UP);
                allocated = allocated.add(share);
            }
            if (share.compareTo(BigDecimal.ZERO) != 0) {
                breakdown.merge(sku.skuCode(), share, BigDecimal::add);
            }
        }
    }
}
