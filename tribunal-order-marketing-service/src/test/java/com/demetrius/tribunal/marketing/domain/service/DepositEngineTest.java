package com.demetrius.tribunal.marketing.domain.service;

import com.demetrius.tribunal.marketing.domain.model.DepositResult;
import com.demetrius.tribunal.marketing.domain.model.DepositRule;
import com.demetrius.tribunal.marketing.domain.model.PackagingType;
import com.demetrius.tribunal.marketing.domain.model.SkuItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DepositEngine 单元测试（M4 押金引擎）。
 */
class DepositEngineTest {

    private final DepositEngine engine = new DepositEngine();

    private static SkuItem sku(String code, String qty) {
        return new SkuItem(code, code, new BigDecimal(qty), new BigDecimal("10"));
    }

    private static DepositRule rule(String id, String skuCode, PackagingType type,
                                    String deposit, boolean includedInPrice) {
        return new DepositRule(id, skuCode, type, new BigDecimal(deposit), includedInPrice, true);
    }

    @Test
    void shouldCalculateDepositForMatchingSkus() {
        var skus = List.of(
                sku("S1", "5"),  // 瓶装押金 2/件 × 5 = 10
                sku("S2", "3")); // 箱装押金 5/件 × 3 = 15
        var rules = List.of(
                rule("D1", "S1", PackagingType.BOTTLE, "2", false),
                rule("D2", "S2", PackagingType.BOX, "5", false));
        DepositResult r = engine.calculate(skus, rules);
        assertEquals(new BigDecimal("25.00"), r.totalDeposit());
        assertEquals(new BigDecimal("10.00"), r.breakdown().get("S1"));
        assertEquals(new BigDecimal("15.00"), r.breakdown().get("S2"));
    }

    @Test
    void shouldSkipDepositIncludedInPrice() {
        var skus = List.of(sku("S1", "5"));
        var rules = List.of(rule("D1", "S1", PackagingType.BOTTLE, "2", true));
        DepositResult r = engine.calculate(skus, rules);
        assertEquals(BigDecimal.ZERO, r.totalDeposit());
        assertTrue(r.breakdown().isEmpty());
    }

    @Test
    void shouldSkipSkuWithoutRule() {
        var skus = List.of(
                sku("S1", "5"),  // 有规则
                sku("S2", "3")); // 无规则
        var rules = List.of(rule("D1", "S1", PackagingType.BOTTLE, "2", false));
        DepositResult r = engine.calculate(skus, rules);
        assertEquals(new BigDecimal("10.00"), r.totalDeposit());
        assertNull(r.breakdown().get("S2"));
    }

    @Test
    void shouldSkipInactiveRule() {
        var skus = List.of(sku("S1", "5"));
        var rules = List.of(new DepositRule("D1", "S1", PackagingType.BOTTLE,
                new BigDecimal("2"), false, false));
        DepositResult r = engine.calculate(skus, rules);
        assertEquals(BigDecimal.ZERO, r.totalDeposit());
    }

    @Test
    void shouldReturnEmptyWhenNoSkus() {
        DepositResult r = engine.calculate(List.of(), List.of(
                rule("D1", "S1", PackagingType.BOTTLE, "2", false)));
        assertEquals(BigDecimal.ZERO, r.totalDeposit());
    }

    @Test
    void shouldReturnEmptyWhenNoRules() {
        var skus = List.of(sku("S1", "5"));
        DepositResult r = engine.calculate(skus, List.of());
        assertEquals(BigDecimal.ZERO, r.totalDeposit());
    }

    @Test
    void shouldHandleAllFivePackagingTypes() {
        var skus = List.of(
                sku("BOTTLE_SKU", "1"),
                sku("BOX_SKU", "1"),
                sku("KEG_SKU", "1"),
                sku("TRAY_SKU", "1"),
                sku("JAR_SKU", "1"));
        var rules = List.of(
                rule("D1", "BOTTLE_SKU", PackagingType.BOTTLE, "1", false),
                rule("D2", "BOX_SKU", PackagingType.BOX, "2", false),
                rule("D3", "KEG_SKU", PackagingType.KEG, "10", false),
                rule("D4", "TRAY_SKU", PackagingType.TRAY, "50", false),
                rule("D5", "JAR_SKU", PackagingType.JAR, "5", false));
        DepositResult r = engine.calculate(skus, rules);
        // 1+2+10+50+5 = 68
        assertEquals(new BigDecimal("68.00"), r.totalDeposit());
        assertEquals(5, r.breakdown().size());
    }

    @Test
    void shouldUseFirstRuleWhenDuplicateSkuCode() {
        var skus = List.of(sku("S1", "1"));
        var rules = List.of(
                rule("D1", "S1", PackagingType.BOTTLE, "3", false),
                rule("D2", "S1", PackagingType.JAR, "5", false));
        DepositResult r = engine.calculate(skus, rules);
        assertEquals(new BigDecimal("3.00"), r.totalDeposit());
    }
}
