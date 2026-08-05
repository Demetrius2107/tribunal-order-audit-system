package com.demetrius.tribunal.order.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 订单业务扩展单元测试（预购 F-312 / 拼车 F-310 / 空包装回收 F-311）。
 */
class OrderBusinessTest {

    private static OrderSku sku(String code, BigDecimal qty, BigDecimal price) {
        return new OrderSku(code, "测试SKU-" + code, qty, price);
    }

    private static List<OrderSku> skus() {
        return List.of(sku("SKU001", BigDecimal.TEN, BigDecimal.valueOf(50))); // 500
    }

    @Test
    @DisplayName("预购订单取消：走专用终态 PRE_ORDER_ENDED（业务文档七节：998）")
    void preOrderCancelShouldEnd() {
        Order order = Order.create(new OrderId("ord-1"), "ORD1", "cust-1",
                OrderType.PRE_ORDER, false, skus());
        assertTrue(order.isPreOrder());
        order.cancel();
        assertEquals(OrderStatus.PRE_ORDER_ENDED, order.getStatus());
    }

    @Test
    @DisplayName("普通订单取消：走 CANCELLED")
    void normalOrderCancelShouldCancel() {
        Order order = Order.create(new OrderId("ord-2"), "ORD2", "cust-1", skus());
        assertFalse(order.isPreOrder());
        order.cancel();
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    @DisplayName("预购终态不可再迁移")
    void preOrderEndedIsTerminal() {
        Order order = Order.create(new OrderId("ord-3"), "ORD3", "cust-1",
                OrderType.PRE_ORDER, false, skus());
        order.cancel();
        assertThrows(IllegalStateException.class, order::confirm);
    }

    @Test
    @DisplayName("拼车订单：可标记参与拼车")
    void carPoolOrderCanJoin() {
        Order order = Order.create(new OrderId("ord-4"), "ORD4", "cust-1",
                OrderType.NORMAL, true, skus());
        assertTrue(order.isCarPooling());
        order.joinCarPool();
        assertTrue(order.isCarPoolJoined());
    }

    @Test
    @DisplayName("非拼车订单不能参与拼车")
    void nonCarPoolCannotJoin() {
        Order order = Order.create(new OrderId("ord-5"), "ORD5", "cust-1", skus());
        assertThrows(IllegalStateException.class, order::joinCarPool);
    }

    @Test
    @DisplayName("已参与拼车的订单不可关闭（CARPOOL_CANNOT_BE_CLOSED）")
    void joinedCarPoolCannotCancel() {
        Order order = Order.create(new OrderId("ord-6"), "ORD6", "cust-1",
                OrderType.NORMAL, true, skus());
        order.joinCarPool();
        assertThrows(IllegalStateException.class, order::cancel);
    }

    @Test
    @DisplayName("空包装回收：押金合计计入应付金额")
    void returnableDepositIncludedInPayable() {
        Order order = Order.create(new OrderId("ord-7"), "ORD7", "cust-1",
                OrderType.NORMAL, false, skus(),
                List.of(new ReturnablePackaging("TRAY", "托盘", BigDecimal.ONE, BigDecimal.valueOf(100))));
        // 应付 = 500（商品） + 100（回收押金） = 600
        assertEquals(0, BigDecimal.valueOf(600).compareTo(order.getPayableAmount()));
        assertEquals(0, BigDecimal.valueOf(100).compareTo(order.getReturnableDepositTotal()));
    }

    @Test
    @DisplayName("空包装回收：数量/押金单价校验")
    void returnableValidation() {
        assertThrows(IllegalArgumentException.class,
                () -> new ReturnablePackaging("TRAY", "托盘", BigDecimal.ZERO, BigDecimal.valueOf(100)));
        assertThrows(IllegalArgumentException.class,
                () -> new ReturnablePackaging("TRAY", "托盘", BigDecimal.ONE, BigDecimal.valueOf(-1)));
    }
}
