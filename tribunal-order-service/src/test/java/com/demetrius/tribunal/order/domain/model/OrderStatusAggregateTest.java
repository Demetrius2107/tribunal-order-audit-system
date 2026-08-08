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
 * M4 父单状态聚合行为单元测试（蓝图 §4.3 / §6.2 验收清单）。
 *
 * <p>覆盖：全部发货/部分发货/全部签收/部分签收/幂等（重复聚合状态不变）。</p>
 */
class OrderStatusAggregateTest {

    private static OrderSku sku(String code, BigDecimal qty, BigDecimal price) {
        return new OrderSku(code, "测试SKU-" + code, qty, price);
    }

    /** 构造一个 SPLITTED 状态的父单 */
    private static Order splittedParent() {
        Order parent = Order.create(
                new OrderId("parent-001"), "ORD1001", "cust-001",
                List.of(sku("SKU001", BigDecimal.TEN, BigDecimal.valueOf(50))));
        parent.confirm();
        parent.markSplitting();
        parent.completeSplit();
        return parent;
    }

    @Test
    @DisplayName("状态聚合：全部子单已发货 → 父单 SPLITTED → SHIPPED")
    void shouldTransitToShippedWhenAllShipped() {
        Order parent = splittedParent();
        boolean changed = parent.aggregateChildStatus(3, 0, 3);

        assertTrue(changed);
        assertEquals(OrderStatus.SHIPPED, parent.getStatus());
    }

    @Test
    @DisplayName("状态聚合：部分子单已发货 → 父单 SPLITTED → PARTIALLY_SHIPPED")
    void shouldTransitToPartiallyShipped() {
        Order parent = splittedParent();
        boolean changed = parent.aggregateChildStatus(1, 0, 3);

        assertTrue(changed);
        assertEquals(OrderStatus.PARTIALLY_SHIPPED, parent.getStatus());
    }

    @Test
    @DisplayName("状态聚合：部分发货后再全部发货 → PARTIALLY_SHIPPED → SHIPPED")
    void shouldTransitFromPartiallyShippedToShipped() {
        Order parent = splittedParent();
        parent.aggregateChildStatus(1, 0, 3); // → PARTIALLY_SHIPPED
        boolean changed = parent.aggregateChildStatus(3, 0, 3);

        assertTrue(changed);
        assertEquals(OrderStatus.SHIPPED, parent.getStatus());
    }

    @Test
    @DisplayName("状态聚合：部分子单已签收 → PARTIALLY_SIGNED")
    void shouldTransitToPartiallySigned() {
        Order parent = splittedParent();
        parent.aggregateChildStatus(3, 0, 3); // → SHIPPED
        boolean changed = parent.aggregateChildStatus(3, 1, 3);

        assertTrue(changed);
        assertEquals(OrderStatus.PARTIALLY_SIGNED, parent.getStatus());
    }

    @Test
    @DisplayName("状态聚合：全部子单已签收 → SIGNED（终态）")
    void shouldTransitToSignedWhenAllSigned() {
        Order parent = splittedParent();
        parent.aggregateChildStatus(3, 0, 3); // → SHIPPED
        boolean changed = parent.aggregateChildStatus(3, 3, 3);

        assertTrue(changed);
        assertEquals(OrderStatus.SIGNED, parent.getStatus());
    }

    @Test
    @DisplayName("状态聚合：无子单发货/签收 → 状态不变（幂等）")
    void shouldRemainUnchangedWhenNoProgress() {
        Order parent = splittedParent();
        boolean changed = parent.aggregateChildStatus(0, 0, 3);

        assertFalse(changed);
        assertEquals(OrderStatus.SPLITTED, parent.getStatus());
    }

    @Test
    @DisplayName("状态聚合：重复回传相同状态 → 状态不变（幂等）")
    void shouldRemainUnchangedWhenSameState() {
        Order parent = splittedParent();
        parent.aggregateChildStatus(3, 0, 3); // → SHIPPED
        boolean changed = parent.aggregateChildStatus(3, 0, 3); // 再次回传，仍 SHIPPED

        assertFalse(changed);
        assertEquals(OrderStatus.SHIPPED, parent.getStatus());
    }

    @Test
    @DisplayName("状态聚合：子单总数为 0 抛异常")
    void shouldRejectZeroTotalChildren() {
        Order parent = splittedParent();
        assertThrows(IllegalArgumentException.class,
                () -> parent.aggregateChildStatus(0, 0, 0));
    }

    @Test
    @DisplayName("拆单工厂：createChild 生成子单 parentOrderId 指向父单")
    void shouldCreateChildOrderWithParentId() {
        Order parent = splittedParent();
        OrderSku childSku = sku("SKU001", BigDecimal.TEN, BigDecimal.valueOf(50));
        childSku.assignWarehouse("WH-A");
        Order child = Order.createChild(new OrderId("child-1"), "ORD1001-01", parent, List.of(childSku));

        assertTrue(child.isChildOrder());
        assertEquals("parent-001", child.getParentOrderId());
        assertEquals(OrderStatus.TO_BE_CONFIRMED, child.getStatus());
        assertEquals("WH-A", child.getSkus().get(0).getWarehouseId());
    }

    @Test
    @DisplayName("明细工厂：withWarehouse 复制明细并绑定仓库")
    void shouldCloneSkuWithWarehouse() {
        OrderSku original = sku("SKU001", BigDecimal.TEN, BigDecimal.valueOf(50));
        OrderSku copy = original.withWarehouse("WH-A");

        assertEquals("SKU001", copy.getSkuCode());
        assertEquals("WH-A", copy.getWarehouseId());
        // 原明细不变
        assertEquals(null, original.getWarehouseId());
    }
}
