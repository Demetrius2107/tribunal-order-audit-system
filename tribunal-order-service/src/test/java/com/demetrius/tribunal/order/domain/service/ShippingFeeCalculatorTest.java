package com.demetrius.tribunal.order.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * F-103 运费计算领域服务单测。
 */
class ShippingFeeCalculatorTest {

    private final ShippingFeeCalculator calculator = new ShippingFeeCalculator();

    @Test
    @DisplayName("满额免邮：商品总额≥500 运费为 0")
    void freeShippingOverThreshold() {
        BigDecimal fee = calculator.calculate(10, new BigDecimal("600.00"));
        assertEquals(0, fee.compareTo(BigDecimal.ZERO), "满额应免邮");
    }

    @Test
    @DisplayName("未满额：基础运费 + 每件加价")
    void baseFeePlusPerItem() {
        // 10 + 2×5 = 20
        BigDecimal fee = calculator.calculate(5, new BigDecimal("300.00"));
        assertEquals(0, fee.compareTo(new BigDecimal("20.00")));
    }

    @Test
    @DisplayName("件数过多：运费封顶 50")
    void feeCappedAtMax() {
        // 10 + 2×30 = 70 → 封顶 50
        BigDecimal fee = calculator.calculate(30, new BigDecimal("300.00"));
        assertEquals(0, fee.compareTo(new BigDecimal("50.00")));
    }

    @Test
    @DisplayName("空单/零金额：运费 0")
    void emptyOrderFree() {
        assertEquals(0, calculator.calculate(0, BigDecimal.ZERO).compareTo(BigDecimal.ZERO));
        assertEquals(0, calculator.calculate(3, null).compareTo(BigDecimal.ZERO));
    }
}
