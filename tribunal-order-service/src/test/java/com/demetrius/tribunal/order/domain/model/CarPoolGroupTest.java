package com.demetrius.tribunal.order.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F-310 拼车组聚合根单测（状态机 + 业务规则）。
 */
class CarPoolGroupTest {

    private CarPoolGroup newGroup() {
        return CarPoolGroup.create("cpg-001", "CP202608160001");
    }

    @Test
    @DisplayName("发起拼车组：初始状态 OPEN、无成员")
    void createInitialState() {
        CarPoolGroup group = newGroup();
        assertEquals(CarPoolGroupStatus.OPEN, group.getStatus());
        assertEquals(0, group.getMemberCount());
        assertEquals("CP202608160001", group.getGroupNo());
    }

    @Test
    @DisplayName("加入成员：OPEN 可加入，成员数递增")
    void joinInOpen() {
        CarPoolGroup group = newGroup();
        group.join("ORD001");
        group.join("ORD002");
        assertEquals(2, group.getMemberCount());
        assertTrue(group.getMemberOrderNos().contains("ORD001"));
    }

    @Test
    @DisplayName("重复加入同一订单被拒")
    void joinDuplicateRejected() {
        CarPoolGroup group = newGroup();
        group.join("ORD001");
        assertThrows(IllegalStateException.class, () -> group.join("ORD001"));
    }

    @Test
    @DisplayName("确认拼车：成员不足 2 被拒")
    void confirmWithTooFewMembersRejected() {
        CarPoolGroup group = newGroup();
        group.join("ORD001");
        assertThrows(IllegalStateException.class, group::confirm);
        assertEquals(CarPoolGroupStatus.OPEN, group.getStatus(), "确认失败应保持 OPEN");
    }

    @Test
    @DisplayName("确认拼车：满 2 成员 → CONFIRMED")
    void confirmWithEnoughMembers() {
        CarPoolGroup group = newGroup();
        group.join("ORD001");
        group.join("ORD002");
        group.confirm();
        assertEquals(CarPoolGroupStatus.CONFIRMED, group.getStatus());
    }

    @Test
    @DisplayName("确认后不可再加入成员（成员锁定）")
    void joinAfterConfirmRejected() {
        CarPoolGroup group = newGroup();
        group.join("ORD001");
        group.join("ORD002");
        group.confirm();
        assertThrows(IllegalStateException.class, () -> group.join("ORD003"));
    }

    @Test
    @DisplayName("关闭拼车：CONFIRMED → CLOSED")
    void closeAfterConfirm() {
        CarPoolGroup group = newGroup();
        group.join("ORD001");
        group.join("ORD002");
        group.confirm();
        group.close();
        assertEquals(CarPoolGroupStatus.CLOSED, group.getStatus());
    }

    @Test
    @DisplayName("取消拼车：OPEN / CONFIRMED 均可取消")
    void cancelAllowedFromOpenAndConfirmed() {
        CarPoolGroup open = newGroup();
        open.cancel();
        assertEquals(CarPoolGroupStatus.CANCELLED, open.getStatus());

        CarPoolGroup confirmed = newGroup();
        confirmed.join("ORD001");
        confirmed.join("ORD002");
        confirmed.confirm();
        confirmed.cancel();
        assertEquals(CarPoolGroupStatus.CANCELLED, confirmed.getStatus());
    }

    @Test
    @DisplayName("终态（CLOSED）不可再迁移")
    void closedIsTerminal() {
        CarPoolGroup group = newGroup();
        group.join("ORD001");
        group.join("ORD002");
        group.confirm();
        group.close();
        assertThrows(IllegalStateException.class, group::cancel);
    }

    @Test
    @DisplayName("还原工厂：保留状态与成员")
    void restoreKeepsStateAndMembers() {
        CarPoolGroup group = CarPoolGroup.restore(
                "cpg-001", "CP202608160001", CarPoolGroupStatus.CONFIRMED,
                java.util.List.of("ORD001", "ORD002"),
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
        assertEquals(CarPoolGroupStatus.CONFIRMED, group.getStatus());
        assertEquals(2, group.getMemberCount());
    }
}
