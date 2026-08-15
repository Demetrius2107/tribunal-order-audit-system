package com.demetrius.tribunal.order.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * F-206 折扣上限策略单测。
 */
class DiscountCapPolicyTest {

    private final DiscountCapPolicy policy = new DiscountCapPolicy();

    @Test
    @DisplayName("折扣未超上限：按原值返回")
    void capWithinLimit() {
        BigDecimal capped = policy.cap(new BigDecimal("1000.00"), new BigDecimal("300.00"));
        assertEquals(0, capped.compareTo(new BigDecimal("300.00")), "未超上限应保持原值");
    }

    @Test
    @DisplayName("折扣超上限：截断到商品总额×50%")
    void capExceedsLimit() {
        BigDecimal capped = policy.cap(new BigDecimal("1000.00"), new BigDecimal("800.00"));
        assertEquals(0, capped.compareTo(new BigDecimal("500.00")), "应截断到 500.00");
    }

    @Test
    @DisplayName("恰好在上限：允许")
    void capExactlyAtLimit() {
        BigDecimal capped = policy.cap(new BigDecimal("1000.00"), new BigDecimal("500.00"));
        assertEquals(0, capped.compareTo(new BigDecimal("500.00")));
    }

    @Test
    @DisplayName("validate 超限抛异常")
    void validateThrowsWhenExceeds() {
        assertThrows(IllegalArgumentException.class,
                () -> policy.validate(new BigDecimal("1000.00"), new BigDecimal("600.00")));
    }

    @Test
    @DisplayName("validate 未超限不抛异常")
    void validatePassesWithinLimit() {
        policy.validate(new BigDecimal("1000.00"), new BigDecimal("400.00"));
    }
}
