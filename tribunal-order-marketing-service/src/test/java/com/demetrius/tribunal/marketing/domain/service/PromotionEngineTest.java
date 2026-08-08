package com.demetrius.tribunal.marketing.domain.service;

import com.demetrius.tribunal.marketing.domain.model.PromotionContext;
import com.demetrius.tribunal.marketing.domain.model.PromotionResult;
import com.demetrius.tribunal.marketing.domain.model.PromotionRule;
import com.demetrius.tribunal.marketing.domain.model.PromotionTargetType;
import com.demetrius.tribunal.marketing.domain.model.PromotionType;
import com.demetrius.tribunal.marketing.domain.model.SkuItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PromotionEngine 单元测试（M4 促销引擎）。
 */
class PromotionEngineTest {

    private final PromotionEngine engine = new PromotionEngine();

    private static final PromotionContext CTX = new PromotionContext("C001", "G01");
    private static final PromotionContext OTHER_CTX = new PromotionContext("C999", "G99");

    private static SkuItem sku(String code, String qty, String price) {
        return new SkuItem(code, code, new BigDecimal(qty), new BigDecimal(price));
    }

    private static PromotionRule fullReduction(String id, String threshold, String reduction,
                                               boolean exclusive, int priority) {
        return new PromotionRule(id, id, "满减", PromotionType.FULL_REDUCTION,
                PromotionTargetType.ALL, null,
                new BigDecimal(threshold), null, new BigDecimal(reduction), null, null,
                null, null, null, exclusive, priority, true, null, null);
    }

    private static PromotionRule discount(String id, String rate, boolean exclusive, int priority) {
        return new PromotionRule(id, id, "折扣", PromotionType.DISCOUNT,
                PromotionTargetType.ALL, null,
                null, new BigDecimal(rate), null, null, null,
                null, null, null, exclusive, priority, true, null, null);
    }

    private static PromotionRule secondHalf(String id, String halfRate, String skuCode) {
        return new PromotionRule(id, id, "第二件半价", PromotionType.SECOND_HALF_PRICE,
                PromotionTargetType.ALL, null,
                null, null, null, new BigDecimal(halfRate), skuCode,
                null, null, null, false, 10, true, null, null);
    }

    private static PromotionRule gift(String id, String threshold, String giftCode, String giftQty) {
        return new PromotionRule(id, id, "满赠", PromotionType.GIFT,
                PromotionTargetType.ALL, null,
                new BigDecimal(threshold), null, null, null, null,
                giftCode, "赠品", new BigDecimal(giftQty), false, 20, true, null, null);
    }

    private static PromotionRule forCustomer(String id, String customerCode, String rate) {
        return new PromotionRule(id, id, "客户专享折扣", PromotionType.DISCOUNT,
                PromotionTargetType.CUSTOMER, customerCode,
                null, new BigDecimal(rate), null, null, null,
                null, null, null, false, 5, true, null, null);
    }

    // ===== 满减 =====

    @Test
    void shouldApplyFullReductionWhenOverThreshold() {
        var skus = List.of(sku("S1", "10", "20"));  // 200
        var rule = fullReduction("R1", "100", "20", false, 1);
        PromotionResult r = engine.calculate(skus, List.of(rule), CTX);
        assertEquals(new BigDecimal("20.00"), r.discountAmount());
        assertEquals(List.of("R1"), r.appliedRuleIds());
        // 分摊：S1 独占，应分摊 20
        assertEquals(new BigDecimal("20.00"), r.skuDiscountBreakdown().get("S1"));
    }

    @Test
    void shouldNotApplyFullReductionWhenUnderThreshold() {
        var skus = List.of(sku("S1", "2", "20"));  // 40
        var rule = fullReduction("R1", "100", "20", false, 1);
        PromotionResult r = engine.calculate(skus, List.of(rule), CTX);
        assertEquals(BigDecimal.ZERO, r.discountAmount());
        assertTrue(r.appliedRuleIds().isEmpty());
    }

    // ===== 折扣 =====

    @Test
    void shouldApplyDiscountRate() {
        var skus = List.of(sku("S1", "5", "40"));  // 200
        var rule = discount("R1", "0.9", false, 1); // 九折 → 减 20
        PromotionResult r = engine.calculate(skus, List.of(rule), CTX);
        assertEquals(new BigDecimal("20.00"), r.discountAmount());
    }

    @Test
    void shouldProrateDiscountAcrossMultipleSkus() {
        var skus = List.of(
                sku("S1", "10", "30"),  // 300 (75%)
                sku("S2", "10", "10")); // 100 (25%)
        var rule = discount("R1", "0.9", false, 1); // 400 九折 → 减 40
        PromotionResult r = engine.calculate(skus, List.of(rule), CTX);
        assertEquals(new BigDecimal("40.00"), r.discountAmount());
        // 分摊：S1=30, S2=10（尾差吸收保证求和=40）
        BigDecimal sum = r.skuDiscountBreakdown().values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("40.00"), sum);
    }

    // ===== 第二件半价 =====

    @Test
    void shouldApplySecondHalfPrice() {
        var skus = List.of(sku("S1", "5", "10")); // 5件，floor(5/2)=2件半价
        var rule = secondHalf("R1", "0.5", "S1");  // 2 × 10 × 0.5 = 10
        PromotionResult r = engine.calculate(skus, List.of(rule), CTX);
        assertEquals(new BigDecimal("10.00"), r.discountAmount());
        assertEquals(new BigDecimal("10.00"), r.skuDiscountBreakdown().get("S1"));
    }

    @Test
    void shouldNotApplySecondHalfPriceWhenQuantityOne() {
        var skus = List.of(sku("S1", "1", "10"));
        var rule = secondHalf("R1", "0.5", "S1");
        PromotionResult r = engine.calculate(skus, List.of(rule), CTX);
        assertEquals(BigDecimal.ZERO, r.discountAmount());
    }

    // ===== 满赠 =====

    @Test
    void shouldProduceGiftWhenOverThreshold() {
        var skus = List.of(sku("S1", "10", "20")); // 200
        var rule = gift("R1", "100", "GIFT001", "2");
        PromotionResult r = engine.calculate(skus, List.of(rule), CTX);
        assertEquals(1, r.giftItems().size());
        assertEquals("GIFT001", r.giftItems().get(0).skuCode());
        assertEquals(new BigDecimal("2"), r.giftItems().get(0).quantity());
        assertEquals(BigDecimal.ZERO, r.discountAmount());
    }

    @Test
    void shouldNotProduceGiftWhenUnderThreshold() {
        var skus = List.of(sku("S1", "2", "20")); // 40
        var rule = gift("R1", "100", "GIFT001", "2");
        PromotionResult r = engine.calculate(skus, List.of(rule), CTX);
        assertTrue(r.giftItems().isEmpty());
    }

    // ===== 叠加 =====

    @Test
    void shouldStackNonExclusiveRules() {
        var skus = List.of(sku("S1", "10", "20")); // 200
        // 满减(优先1)：满100减20 → 剩 180
        // 折扣(优先2)：九折 → 180×0.1=18
        // 总折扣 = 20 + 18 = 38
        var rules = List.of(
                fullReduction("R1", "100", "20", false, 1),
                discount("R2", "0.9", false, 2));
        PromotionResult r = engine.calculate(skus, rules, CTX);
        assertEquals(new BigDecimal("38.00"), r.discountAmount());
        assertEquals(List.of("R1", "R2"), r.appliedRuleIds());
    }

    // ===== 互斥 =====

    @Test
    void shouldShortCircuitOnExclusiveRule() {
        var skus = List.of(sku("S1", "10", "20")); // 200
        var rules = List.of(
                fullReduction("R1", "100", "50", true, 1),  // 互斥，应用后终止
                discount("R2", "0.9", false, 2));            // 被短路，不应用
        PromotionResult r = engine.calculate(skus, rules, CTX);
        assertEquals(new BigDecimal("50.00"), r.discountAmount());
        assertEquals(List.of("R1"), r.appliedRuleIds());
    }

    // ===== 适用对象匹配 =====

    @Test
    void shouldMatchCustomerSpecificRule() {
        var skus = List.of(sku("S1", "10", "20"));
        var rule = forCustomer("R1", "C001", "0.8"); // C001 专享八折
        PromotionResult r = engine.calculate(skus, List.of(rule), CTX);
        assertEquals(new BigDecimal("40.00"), r.discountAmount()); // 200×0.2=40
    }

    @Test
    void shouldNotMatchOtherCustomerRule() {
        var skus = List.of(sku("S1", "10", "20"));
        var rule = forCustomer("R1", "C001", "0.8");
        PromotionResult r = engine.calculate(skus, List.of(rule), OTHER_CTX);
        assertEquals(BigDecimal.ZERO, r.discountAmount());
    }

    // ===== 有效期 =====

    @Test
    void shouldSkipInactiveRule() {
        var skus = List.of(sku("S1", "10", "20"));
        var rule = new PromotionRule("R1", "R1", "折扣", PromotionType.DISCOUNT,
                PromotionTargetType.ALL, null,
                null, new BigDecimal("0.9"), null, null, null,
                null, null, null, false, 1, false, null, null);
        PromotionResult r = engine.calculate(skus, List.of(rule), CTX);
        assertEquals(BigDecimal.ZERO, r.discountAmount());
    }

    @Test
    void shouldSkipExpiredRule() {
        var skus = List.of(sku("S1", "10", "20"));
        var rule = new PromotionRule("R1", "R1", "折扣", PromotionType.DISCOUNT,
                PromotionTargetType.ALL, null,
                null, new BigDecimal("0.9"), null, null, null,
                null, null, null, false, 1, true,
                LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(1));
        PromotionResult r = engine.calculate(skus, List.of(rule), CTX);
        assertEquals(BigDecimal.ZERO, r.discountAmount());
    }

    // ===== 边界 =====

    @Test
    void shouldReturnEmptyWhenNoRules() {
        var skus = List.of(sku("S1", "10", "20"));
        PromotionResult r = engine.calculate(skus, List.of(), CTX);
        assertEquals(BigDecimal.ZERO, r.discountAmount());
        assertTrue(r.appliedRuleIds().isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenDiscountExceedsSubtotal() {
        var skus = List.of(sku("S1", "1", "10")); // 10
        var rule = fullReduction("R1", "5", "100", false, 1); // 满5减100，但只剩10
        PromotionResult r = engine.calculate(skus, List.of(rule), CTX);
        // 折扣不会超过剩余可折扣金额
        assertEquals(new BigDecimal("10.00"), r.discountAmount());
    }
}
