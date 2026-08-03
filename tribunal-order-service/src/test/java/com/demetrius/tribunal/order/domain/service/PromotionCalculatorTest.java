package com.demetrius.tribunal.order.domain.service;

import com.demetrius.tribunal.order.domain.model.Order;
import com.demetrius.tribunal.order.domain.model.OrderId;
import com.demetrius.tribunal.order.domain.model.OrderSku;
import com.demetrius.tribunal.order.domain.model.PromotionRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 促销计算领域服务单元测试（F-202，业务文档二节）。
 */
class PromotionCalculatorTest {

    private final PromotionCalculator calculator = new PromotionCalculator();

    private static Order orderWithSkus() {
        return Order.create(
                new OrderId("ord-001"),
                "ORD1001",
                "cust-001",
                List.of(
                        new OrderSku("SKU001", "商品A", BigDecimal.TEN, BigDecimal.valueOf(50)),   // 行金额 500
                        new OrderSku("SKU002", "商品B", BigDecimal.valueOf(5), BigDecimal.valueOf(100)))); // 行金额 500
    }

    private static PromotionRule customerPromo(String sku, String rate) {
        // 客户型促销：仅对 cust-001 生效，作用于 SKU001，折扣率 10%
        return new PromotionRule(PromotionRule.TYPE_CUSTOMER, "cust-001",
                Set.of(sku), new BigDecimal(rate));
    }

    @Test
    @DisplayName("客户型促销命中：SKU001 折扣 10% → 折扣金额 50（500×0.1）")
    void shouldApplyCustomerPromotion() {
        Order order = orderWithSkus();
        calculator.applyPromotions(order,
                List.of(customerPromo("SKU001", "0.10")), "cust-001", null);

        assertEquals(0, new BigDecimal("50").compareTo(order.getDiscountAmount()));
        // 应付 = 1000 - 50 = 950
        assertEquals(0, new BigDecimal("950").compareTo(order.getPayableAmount()));
    }

    @Test
    @DisplayName("非目标客户命中促销：不计算折扣")
    void shouldSkipPromotionForOtherCustomer() {
        Order order = orderWithSkus();
        calculator.applyPromotions(order,
                List.of(customerPromo("SKU001", "0.10")), "cust-999", null);

        assertEquals(0, BigDecimal.ZERO.compareTo(order.getDiscountAmount()));
    }

    @Test
    @DisplayName("客户组型促销：按客户组 ID 匹配")
    void shouldApplyGroupPromotion() {
        Order order = orderWithSkus();
        PromotionRule groupRule = new PromotionRule(PromotionRule.TYPE_GROUP, "group-A",
                Set.of("SKU002"), new BigDecimal("0.20")); // SKU002 行金额 500 × 20% = 100

        calculator.applyPromotions(order, List.of(groupRule), "cust-001", "group-A");

        assertEquals(0, new BigDecimal("100").compareTo(order.getDiscountAmount()));
    }

    @Test
    @DisplayName("多促销命中同一 SKU：取最大折扣率，不叠加")
    void shouldTakeMaxRateNotSum() {
        Order order = orderWithSkus();
        List<PromotionRule> rules = List.of(
                customerPromo("SKU001", "0.10"),
                customerPromo("SKU001", "0.15")); // 取 15%：500×0.15=75

        calculator.applyPromotions(order, rules, "cust-001", null);

        assertEquals(0, new BigDecimal("75").compareTo(order.getDiscountAmount()));
    }

    @Test
    @DisplayName("无促销规则：不改变金额")
    void shouldNoOpWithoutRules() {
        Order order = orderWithSkus();
        calculator.applyPromotions(order, List.of(), "cust-001", null);
        assertEquals(0, new BigDecimal("1000").compareTo(order.getPayableAmount()));
    }
}
