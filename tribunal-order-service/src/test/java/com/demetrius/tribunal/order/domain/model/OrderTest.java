package com.demetrius.tribunal.order.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 订单聚合行为单元测试。
 *
 * <p>覆盖：创建初始状态、合法迁移、重复迁移（幂等）、非法迁移、参数校验。</p>
 */
class OrderTest {

    private static OrderSku sku(String code, BigDecimal qty, BigDecimal price) {
        return new OrderSku(code, "测试SKU-" + code, qty, price);
    }

    private static Order newOrder() {
        return Order.create(
                new OrderId("ord-001"),
                "ORD1001",
                "cust-001",
                List.of(sku("SKU001", BigDecimal.TEN, BigDecimal.valueOf(50))));
    }

    @Test
    @DisplayName("创建订单：初始状态为待确认，应付金额=Σ明细金额")
    void shouldCreateWithToBeConfirmedAndAmount() {
        Order order = newOrder();
        assertEquals(OrderStatus.TO_BE_CONFIRMED, order.getStatus());
        assertEquals(0, BigDecimal.valueOf(500).compareTo(order.getPayableAmount()));
    }

    @Test
    @DisplayName("合法迁移：待确认 → 已确认（confirm 成功）")
    void shouldConfirmFromToBeConfirmed() {
        Order order = newOrder();
        order.confirm();
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    @DisplayName("幂等：已确认后再 confirm 抛异常（重复状态回传被拒绝）")
    void shouldRejectDoubleConfirm() {
        Order order = newOrder();
        order.confirm();
        assertThrows(IllegalStateException.class, order::confirm);
    }

    @Test
    @DisplayName("非法迁移：已拒绝后不可 confirm")
    void shouldRejectConfirmAfterReject() {
        Order order = newOrder();
        order.reject("信用不足");
        assertThrows(IllegalStateException.class, order::confirm);
    }

    @Test
    @DisplayName("审单拒绝：记录拒绝原因")
    void shouldRecordRejectReason() {
        Order order = newOrder();
        order.reject("客户要求取消");
        assertEquals(OrderStatus.REJECTED, order.getStatus());
        assertEquals("客户要求取消", order.getRejectReason());
    }

    @Test
    @DisplayName("参数校验：空明细不允许创建")
    void shouldRejectEmptySkus() {
        assertThrows(IllegalArgumentException.class, () -> Order.create(
                new OrderId("ord-002"), "ORD1002", "cust-001", List.of()));
    }

    @Test
    @DisplayName("参数校验：数量必须大于0")
    void shouldRejectNonPositiveQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> sku("SKU002", BigDecimal.ZERO, BigDecimal.valueOf(50)));
    }

    @Test
    @DisplayName("完整链路：下单 → 审单通过 → 转单 → 发货 → 签收")
    void shouldRunFullLifecycle() {
        Order order = newOrder();
        order.confirm();
        order.startTransfer();
        order.transferSuccess();
        order.ship();
        order.sign();
        assertEquals(OrderStatus.SIGNED, order.getStatus());
    }
}
