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

    @Test
    @DisplayName("折扣池抵扣：应付金额 = 总金额 - 折扣 - 折扣池抵扣")
    void shouldApplyDiscountPoolDeduction() {
        Order order = newOrder(); // 总金额 500
        order.applyDiscount(BigDecimal.valueOf(100));
        order.applyDiscountPoolDeduction(BigDecimal.valueOf(50));
        assertEquals(0, BigDecimal.valueOf(350).compareTo(order.getPayableAmount()));
    }

    @Test
    @DisplayName("押金与税：应付金额 = 总金额 - 折扣 + 押金 + 税")
    void shouldApplyDepositAndTax() {
        Order order = newOrder(); // 总金额 500
        order.applyDiscount(BigDecimal.valueOf(100));
        order.applyDeposit(BigDecimal.valueOf(50));
        order.applyTax(BigDecimal.valueOf(30));
        // 500 - 100 + 50 + 30 = 480
        assertEquals(0, BigDecimal.valueOf(480).compareTo(order.getPayableAmount()));
    }

    @Test
    @DisplayName("折扣池抵扣为负拒绝")
    void shouldRejectNegativeDeduction() {
        Order order = newOrder();
        assertThrows(IllegalArgumentException.class,
                () -> order.applyDiscountPoolDeduction(BigDecimal.valueOf(-1)));
    }

    @Test
    @DisplayName("折扣+折扣池抵扣超过总金额拒绝（应付为负）")
    void shouldRejectDeductionOverTotal() {
        Order order = newOrder(); // 总金额 500
        assertThrows(IllegalArgumentException.class, () -> {
            order.applyDiscount(BigDecimal.valueOf(400));
            order.applyDiscountPoolDeduction(BigDecimal.valueOf(200)); // 500-400-200 < 0
        });
    }

    @Test
    @DisplayName("运费：应付金额 = 总金额 + 运费（F-103）")
    void shouldApplyShippingFee() {
        Order order = newOrder(); // 总金额 500
        order.applyShippingFee(BigDecimal.valueOf(100));
        assertEquals(0, BigDecimal.valueOf(600).compareTo(order.getPayableAmount()));
        assertEquals(0, BigDecimal.valueOf(100).compareTo(order.getShippingFee()));
    }

    @Test
    @DisplayName("运费为负拒绝")
    void shouldRejectNegativeShippingFee() {
        Order order = newOrder();
        assertThrows(IllegalArgumentException.class,
                () -> order.applyShippingFee(BigDecimal.valueOf(-1)));
    }

    @Test
    @DisplayName("完整金额链路：总金额 - 折扣 - 折扣池抵扣 + 押金 + 税 + 运费")
    void shouldCalcFullPayableChain() {
        Order order = newOrder(); // 总金额 500
        order.applyDiscount(BigDecimal.valueOf(100));
        order.applyDiscountPoolDeduction(BigDecimal.valueOf(50));
        order.applyDeposit(BigDecimal.valueOf(20));
        order.applyTax(BigDecimal.valueOf(30));
        order.applyShippingFee(BigDecimal.valueOf(100));
        // 500 - 100 - 50 + 20 + 30 + 100 = 500
        assertEquals(0, BigDecimal.valueOf(500).compareTo(order.getPayableAmount()));
    }
}
