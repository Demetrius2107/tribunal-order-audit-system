package com.demetrius.tribunal.order.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F-312 预购活动聚合根单测（状态机 + 保证金/独立计价规则）。
 */
class PreOrderActivityTest {

    private final LocalDateTime start = LocalDateTime.of(2026, 8, 1, 0, 0);
    private final LocalDateTime end = LocalDateTime.of(2026, 8, 31, 23, 59);

    private PreOrderActivity newActivity() {
        return PreOrderActivity.create(
                "pre-001", "PRE202608160001", "夏季预购",
                List.of("SKU001", "SKU002"),
                new BigDecimal("0.3000"), new BigDecimal("0.9000"),
                start, end);
    }

    @Test
    @DisplayName("创建预购活动：初始状态 DRAFT、保证金比例 0.3、折扣率 0.9")
    void createInitialState() {
        PreOrderActivity activity = newActivity();
        assertEquals(PreOrderActivityStatus.DRAFT, activity.getStatus());
        assertEquals(0, new BigDecimal("0.3000").compareTo(activity.getDepositRate()));
        assertEquals(0, new BigDecimal("0.9000").compareTo(activity.getDiscountRate()));
    }

    @Test
    @DisplayName("上线：DRAFT → ACTIVE")
    void activate() {
        PreOrderActivity activity = newActivity();
        activity.activate();
        assertEquals(PreOrderActivityStatus.ACTIVE, activity.getStatus());
    }

    @Test
    @DisplayName("草稿状态参与被拒")
    void participateInDraftRejected() {
        PreOrderActivity activity = newActivity();
        assertThrows(IllegalStateException.class,
                () -> activity.validateParticipate("SKU001", start.plusDays(1)));
    }

    @Test
    @DisplayName("进行中 + 有效期内 + SKU 在范围：可参与")
    void participateValid() {
        PreOrderActivity activity = newActivity();
        activity.activate();
        activity.validateParticipate("SKU001", start.plusDays(1));
    }

    @Test
    @DisplayName("SKU 不在活动范围：参与被拒")
    void participateOutOfScopeSkuRejected() {
        PreOrderActivity activity = newActivity();
        activity.activate();
        assertThrows(IllegalStateException.class,
                () -> activity.validateParticipate("SKU999", start.plusDays(1)));
    }

    @Test
    @DisplayName("活动结束时间之外：参与被拒")
    void participateOutOfWindowRejected() {
        PreOrderActivity activity = newActivity();
        activity.activate();
        assertThrows(IllegalStateException.class,
                () -> activity.validateParticipate("SKU001", end.plusDays(1)));
    }

    @Test
    @DisplayName("预购价 = 原价 × 折扣率（独立计价口径）")
    void preOrderPriceAppliesDiscountRate() {
        PreOrderActivity activity = newActivity();
        // 100 × 0.9 = 90
        BigDecimal price = activity.preOrderPrice(new BigDecimal("100.00"));
        assertEquals(0, price.compareTo(new BigDecimal("90.00")));
    }

    @Test
    @DisplayName("保证金 = 金额 × 保证金比例（1000 × 0.3 = 300）")
    void depositAmount() {
        PreOrderActivity activity = newActivity();
        BigDecimal deposit = activity.depositAmount(new BigDecimal("1000.00"));
        assertEquals(0, deposit.compareTo(new BigDecimal("300.00")));
    }

    @Test
    @DisplayName("补缴 = 金额 - 保证金（1000 - 300 = 700）")
    void supplementAmount() {
        PreOrderActivity activity = newActivity();
        BigDecimal supplement = activity.supplementAmount(new BigDecimal("1000.00"));
        assertEquals(0, supplement.compareTo(new BigDecimal("700.00")));
    }

    @Test
    @DisplayName("结束：ACTIVE → ENDED（终态）")
    void endActivity() {
        PreOrderActivity activity = newActivity();
        activity.activate();
        activity.end();
        assertEquals(PreOrderActivityStatus.ENDED, activity.getStatus());
        assertThrows(IllegalStateException.class, activity::cancel);
    }

    @Test
    @DisplayName("取消：DRAFT / ACTIVE 均可取消（终态）")
    void cancelAllowedFromDraftAndActive() {
        PreOrderActivity draft = newActivity();
        draft.cancel();
        assertEquals(PreOrderActivityStatus.CANCELLED, draft.getStatus());

        PreOrderActivity active = newActivity();
        active.activate();
        active.cancel();
        assertEquals(PreOrderActivityStatus.CANCELLED, active.getStatus());
    }

    @Test
    @DisplayName("非法参数被拒：保证金比例越界 / 结束早于开始")
    void invalidArgsRejected() {
        assertThrows(IllegalArgumentException.class, () -> PreOrderActivity.create(
                "pre-x", "PRE001", "非法活动", List.of("SKU001"),
                new BigDecimal("1.5"), new BigDecimal("0.9"), start, end));
        assertThrows(IllegalArgumentException.class, () -> PreOrderActivity.create(
                "pre-y", "PRE002", "非法活动", List.of("SKU001"),
                new BigDecimal("0.3"), new BigDecimal("0.9"), end, start));
    }

    @Test
    @DisplayName("还原工厂：保留状态与配置")
    void restoreKeepsState() {
        PreOrderActivity activity = PreOrderActivity.restore(
                "pre-001", "PRE202608160001", "夏季预购",
                List.of("SKU001"), new BigDecimal("0.3000"), new BigDecimal("0.9000"),
                start, end, PreOrderActivityStatus.ACTIVE, start, start);
        assertEquals(PreOrderActivityStatus.ACTIVE, activity.getStatus());
        assertTrue(activity.getSkuCodes().contains("SKU001"));
    }
}
