package com.demetrius.tribunal.order.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 订单状态机单元测试（★核心测试）。
 *
 * <p>覆盖三类场景：合法迁移、非法迁移、重复状态回传（幂等判断）。</p>
 */
class OrderStatusTest {

    @Test
    @DisplayName("合法迁移：待确认 → 已确认（审单通过）")
    void shouldAllowConfirmedFromToBeConfirmed() {
        assertTrue(OrderStatus.TO_BE_CONFIRMED.canTransitTo(OrderStatus.CONFIRMED));
    }

    @Test
    @DisplayName("合法迁移：待确认 → 已拒绝（审单拒绝）")
    void shouldAllowRejectedFromToBeConfirmed() {
        assertTrue(OrderStatus.TO_BE_CONFIRMED.canTransitTo(OrderStatus.REJECTED));
    }

    @Test
    @DisplayName("合法迁移：已确认 → 转单中")
    void shouldAllowTransferringFromConfirmed() {
        assertTrue(OrderStatus.CONFIRMED.canTransitTo(OrderStatus.TRANSFERRING));
    }

    @Test
    @DisplayName("合法迁移：已转单 → 已发货")
    void shouldAllowShippedFromTransferred() {
        assertTrue(OrderStatus.TRANSFERRED.canTransitTo(OrderStatus.SHIPPED));
    }

    @Test
    @DisplayName("非法迁移：已拒绝 → 已确认（拒绝后不可再通过）")
    void shouldRejectConfirmFromRejected() {
        assertFalse(OrderStatus.REJECTED.canTransitTo(OrderStatus.CONFIRMED));
    }

    @Test
    @DisplayName("非法迁移：已签收 → 已发货（终态不可回退）")
    void shouldRejectShipFromSigned() {
        assertFalse(OrderStatus.SIGNED.canTransitTo(OrderStatus.SHIPPED));
    }

    @Test
    @DisplayName("非法迁移：待确认 → 已发货（跳过中间状态）")
    void shouldRejectSkipStates() {
        assertFalse(OrderStatus.TO_BE_CONFIRMED.canTransitTo(OrderStatus.SHIPPED));
    }

    @Test
    @DisplayName("幂等：已确认 → 已确认（重复状态回传被拒绝）")
    void shouldRejectSameStatusTransition() {
        assertFalse(OrderStatus.CONFIRMED.canTransitTo(OrderStatus.CONFIRMED));
    }

    @Test
    @DisplayName("幂等：已发货 → 已发货（重复回传被拒绝）")
    void shouldRejectSameShippedStatus() {
        assertFalse(OrderStatus.SHIPPED.canTransitTo(OrderStatus.SHIPPED));
    }

    @Test
    @DisplayName("终态不可再迁移：已签收/已拒绝/已取消均无出口")
    void shouldHaveNoTransitionFromTerminalStates() {
        assertFalse(OrderStatus.SIGNED.canTransitTo(OrderStatus.CANCELLED));
        assertFalse(OrderStatus.REJECTED.canTransitTo(OrderStatus.CANCELLED));
        assertFalse(OrderStatus.CANCELLED.canTransitTo(OrderStatus.TO_BE_CONFIRMED));
    }
}
