package com.demetrius.tribunal.order.domain.service;

import com.demetrius.tribunal.order.domain.model.Order;
import com.demetrius.tribunal.order.domain.model.OrderId;
import com.demetrius.tribunal.order.domain.model.OrderSku;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 订单金额计算领域服务单元测试（F-202 金额规则）。
 */
class OrderAmountCalculatorTest {

    private final OrderAmountCalculator calculator = new OrderAmountCalculator();

    private static Order newOrder() {
        return Order.create(
                new OrderId("ord-001"),
                "ORD1001",
                "cust-001",
                List.of(
                        new OrderSku("SKU001", "商品A", BigDecimal.TEN, BigDecimal.valueOf(50)),
                        new OrderSku("SKU002", "商品B", BigDecimal.valueOf(5), BigDecimal.valueOf(100))));
    }

    @Test
    @DisplayName("总金额 = Σ(明细数量 × 单价)：10×50 + 5×100 = 1000")
    void shouldCalcTotalAmount() {
        Order order = newOrder();
        assertEquals(0, BigDecimal.valueOf(1000).compareTo(order.getTotalAmount()));
    }

    @Test
    @DisplayName("应付金额 = 总金额 - 折扣金额")
    void shouldCalcPayableAmount() {
        Order order = newOrder();
        order.applyDiscount(BigDecimal.valueOf(100));
        assertEquals(0, BigDecimal.valueOf(900).compareTo(order.getPayableAmount()));
    }

    @Test
    @DisplayName("重新计价：按新价格表覆盖明细并重算金额")
    void shouldRepriceWithNewPrice() {
        Order order = newOrder();
        Map<String, BigDecimal> priceBySku = Map.of(
                "SKU001", BigDecimal.valueOf(60),   // 10×60 = 600
                "SKU002", BigDecimal.valueOf(120)); // 5×120 = 600
        calculator.reprice(order, priceBySku);
        assertEquals(0, BigDecimal.valueOf(1200).compareTo(order.getTotalAmount()));
    }

    @Test
    @DisplayName("重新计价：未出现在价格表中的 SKU 保持原价")
    void shouldKeepPriceWhenAbsent() {
        Order order = newOrder();
        calculator.reprice(order, Map.of("SKU001", BigDecimal.valueOf(60)));
        assertEquals(0, BigDecimal.valueOf(1100).compareTo(order.getTotalAmount()));
    }
}
