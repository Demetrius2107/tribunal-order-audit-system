package com.demetrius.tribunal.order.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 改单行为单元测试（F-309：仅待确认可改，明细修改 + 金额重算）。
 */
class OrderModifyTest {

    private static Order pendingOrder() {
        return Order.create(
                new OrderId("ord-001"),
                "ORD1001",
                "cust-001",
                List.of(new OrderSku("SKU001", "商品A", BigDecimal.TEN, BigDecimal.valueOf(50)))); // 500
    }

    @Test
    @DisplayName("待确认状态可改：替换明细后金额重算")
    void shouldModifyInToBeConfirmed() {
        Order order = pendingOrder();
        order.modifySkus(List.of(
                new OrderSku("SKU002", "商品B", BigDecimal.valueOf(5), BigDecimal.valueOf(100)))); // 500

        assertEquals(1, order.getSkus().size());
        assertEquals("SKU002", order.getSkus().get(0).getSkuCode());
        assertEquals(0, new BigDecimal("500").compareTo(order.getPayableAmount()));
    }

    @Test
    @DisplayName("已确认订单不允许改单")
    void shouldRejectModifyAfterConfirm() {
        Order order = pendingOrder();
        order.confirm();

        assertThrows(IllegalStateException.class,
                () -> order.modifySkus(List.of(new OrderSku("SKU002", "商品B",
                        BigDecimal.valueOf(5), BigDecimal.valueOf(100)))));
    }

    @Test
    @DisplayName("修改后明细为空拒绝")
    void shouldRejectEmptySkus() {
        Order order = pendingOrder();
        assertThrows(IllegalArgumentException.class, () -> order.modifySkus(List.of()));
    }

    @Test
    @DisplayName("修改明细后折扣/押金/税字段保留并参与应付计算")
    void shouldKeepAmountFieldsOnModify() {
        Order order = pendingOrder();
        order.applyDiscount(new BigDecimal("100"));
        order.applyDeposit(new BigDecimal("50"));
        order.applyTax(new BigDecimal("30"));
        // 应付 = 500 - 100 + 50 + 30 = 480

        order.modifySkus(List.of(new OrderSku("SKU002", "商品B",
                BigDecimal.valueOf(5), BigDecimal.valueOf(100)))); // 500

        assertEquals(0, new BigDecimal("480").compareTo(order.getPayableAmount()));
    }
}
