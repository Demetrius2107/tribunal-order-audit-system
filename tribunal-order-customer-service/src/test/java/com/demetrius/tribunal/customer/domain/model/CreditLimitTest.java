package com.demetrius.tribunal.customer.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 信用额度占用/释放单元测试（F-403）。
 */
class CreditLimitTest {

    private final CreditLimit limit = new CreditLimit(BigDecimal.valueOf(1000), BigDecimal.ZERO);

    @Test
    @DisplayName("占用信用：已占用增加、可用减少")
    void shouldOccupy() {
        CreditLimit after = limit.occupy(BigDecimal.valueOf(300));
        assertEquals(0, BigDecimal.valueOf(300).compareTo(after.used()));
        assertEquals(0, BigDecimal.valueOf(700).compareTo(after.getAvailable()));
    }

    @Test
    @DisplayName("释放信用：已占用减少、可用恢复")
    void shouldRelease() {
        CreditLimit occupied = limit.occupy(BigDecimal.valueOf(300));
        CreditLimit released = occupied.release(BigDecimal.valueOf(100));
        assertEquals(0, BigDecimal.valueOf(200).compareTo(released.used()));
        assertEquals(0, BigDecimal.valueOf(800).compareTo(released.getAvailable()));
    }

    @Test
    @DisplayName("可用信用足够时 hasEnoughFor 为 true")
    void shouldHasEnoughFor() {
        assertTrue(limit.hasEnoughFor(BigDecimal.valueOf(900)));
    }

    @Test
    @DisplayName("信用额度为负拒绝创建")
    void shouldRejectNegativeLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> new CreditLimit(BigDecimal.valueOf(-1), BigDecimal.ZERO));
    }
}
