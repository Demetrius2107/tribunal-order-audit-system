package com.demetrius.tribunal.order.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M4 状态机补全单元测试。
 *
 * <p>覆盖新增的寻源拆单相关状态迁移规则。</p>
 */
class OrderStatusM4Test {

    @Test
    @DisplayName("M4 合法迁移：已确认 → 拆单中")
    void shouldAllowSplittingFromConfirmed() {
        assertTrue(OrderStatus.CONFIRMED.canTransitTo(OrderStatus.SPLITTING));
    }

    @Test
    @DisplayName("M4 合法迁移：拆单中 → 已拆单")
    void shouldAllowSplittedFromSplitting() {
        assertTrue(OrderStatus.SPLITTING.canTransitTo(OrderStatus.SPLITTED));
    }

    @Test
    @DisplayName("M4 合法迁移：拆单中 → 已确认（回退，无需分仓）")
    void shouldAllowRollbackFromSplitting() {
        assertTrue(OrderStatus.SPLITTING.canTransitTo(OrderStatus.CONFIRMED));
    }

    @Test
    @DisplayName("M4 合法迁移：已拆单 → 部分发货")
    void shouldAllowPartiallyShippedFromSplitted() {
        assertTrue(OrderStatus.SPLITTED.canTransitTo(OrderStatus.PARTIALLY_SHIPPED));
    }

    @Test
    @DisplayName("M4 合法迁移：已拆单 → 已发货（子单全部发货）")
    void shouldAllowShippedFromSplitted() {
        assertTrue(OrderStatus.SPLITTED.canTransitTo(OrderStatus.SHIPPED));
    }

    @Test
    @DisplayName("M4 合法迁移：部分发货 → 部分签收")
    void shouldAllowPartiallySignedFromPartiallyShipped() {
        assertTrue(OrderStatus.PARTIALLY_SHIPPED.canTransitTo(OrderStatus.PARTIALLY_SIGNED));
    }

    @Test
    @DisplayName("M4 合法迁移：部分发货 → 已发货（其余子单发货）")
    void shouldAllowShippedFromPartiallyShipped() {
        assertTrue(OrderStatus.PARTIALLY_SHIPPED.canTransitTo(OrderStatus.SHIPPED));
    }

    @Test
    @DisplayName("M4 合法迁移：部分签收 → 已签收")
    void shouldAllowSignedFromPartiallySigned() {
        assertTrue(OrderStatus.PARTIALLY_SIGNED.canTransitTo(OrderStatus.SIGNED));
    }

    @Test
    @DisplayName("M4 非法迁移：待确认 → 拆单中（未确认不可拆单）")
    void shouldRejectSplittingFromToBeConfirmed() {
        assertFalse(OrderStatus.TO_BE_CONFIRMED.canTransitTo(OrderStatus.SPLITTING));
    }

    @Test
    @DisplayName("M4 非法迁移：已拆单 → 部分签收（跨级，必须先部分发货）")
    void shouldRejectPartiallySignedFromSplitted() {
        assertFalse(OrderStatus.SPLITTED.canTransitTo(OrderStatus.PARTIALLY_SIGNED));
    }

    @Test
    @DisplayName("M4 非法迁移：已签收 → 部分签收（终态不可回退）")
    void shouldRejectPartiallySignedFromSigned() {
        assertFalse(OrderStatus.SIGNED.canTransitTo(OrderStatus.PARTIALLY_SIGNED));
    }
}
