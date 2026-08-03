package com.demetrius.tribunal.order.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 订单金额扩展行为测试（F-202 折扣参与计算）。
 */
class OrderAmountTest {

    private static Order newOrder() {
        return Order.create(
                new OrderId("ord-001"),
                "ORD1001",
                "cust-001",
                List.of(new OrderSku("SKU001", "商品A", BigDecimal.TEN, BigDecimal.valueOf(50))));
    }

    @Test
    @DisplayName("应用折扣：应付金额 = 总金额 - 折扣金额")
    void shouldApplyDiscount() {
        Order order = newOrder();
        order.applyDiscount(BigDecimal.valueOf(200));
        assertEquals(0, BigDecimal.valueOf(300).compareTo(order.getPayableAmount()));
        assertEquals(0, BigDecimal.valueOf(200).compareTo(order.getDiscountAmount()));
    }

    @Test
    @DisplayName("折扣为负拒绝")
    void shouldRejectNegativeDiscount() {
        Order order = newOrder();
        assertThrows(IllegalArgumentException.class,
                () -> order.applyDiscount(BigDecimal.valueOf(-1)));
    }

    @Test
    @DisplayName("折扣超过总金额拒绝")
    void shouldRejectDiscountOverTotal() {
        Order order = newOrder();
        assertThrows(IllegalArgumentException.class,
                () -> order.applyDiscount(BigDecimal.valueOf(501)));
    }

    @Test
    @DisplayName("重新定价：单价变化后金额同步重算")
    void shouldRepriceSku() {
        Order order = newOrder();
        order.getSkus().get(0).reprice(BigDecimal.valueOf(60));
        order.recalculateAmounts();
        assertEquals(0, BigDecimal.valueOf(600).compareTo(order.getTotalAmount()));
    }
}
