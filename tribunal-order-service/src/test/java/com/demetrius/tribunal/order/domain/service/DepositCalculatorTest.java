package com.demetrius.tribunal.order.domain.service;

import com.demetrius.tribunal.order.domain.model.Order;
import com.demetrius.tribunal.order.domain.model.OrderId;
import com.demetrius.tribunal.order.domain.model.OrderSku;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 押金计算领域服务单元测试（F-205，业务文档四节：按 SKU-客户押金配置计算押金）。
 */
class DepositCalculatorTest {

    private final DepositCalculator calculator = new DepositCalculator();

    private static Order orderWithSkus() {
        return Order.create(
                new OrderId("ord-001"),
                "ORD1001",
                "cust-001",
                List.of(
                        new OrderSku("SKU001", "商品A", BigDecimal.TEN, BigDecimal.valueOf(50)),    // 数量 10
                        new OrderSku("SKU002", "商品B", BigDecimal.valueOf(5), BigDecimal.valueOf(100)))); // 数量 5
    }

    @Test
    @DisplayName("按配置计算押金：SKU001 押金 2/件 ×10 + SKU002 押金 3/件 ×5 = 35")
    void shouldCalcDepositFromConfig() {
        Order order = orderWithSkus();
        Map<String, BigDecimal> config = Map.of(
                "SKU001", new BigDecimal("2"),
                "SKU002", new BigDecimal("3"));

        BigDecimal deposit = calculator.applyDeposit(order, config);

        assertEquals(0, new BigDecimal("35").compareTo(deposit));
        assertEquals(0, new BigDecimal("35").compareTo(order.getDepositAmount()));
    }

    @Test
    @DisplayName("押金参与应付金额：总金额 1000 + 押金 35 = 1035")
    void shouldIncludeDepositInPayable() {
        Order order = orderWithSkus(); // 总金额 1000
        calculator.applyDeposit(order, Map.of("SKU001", new BigDecimal("2"), "SKU002", new BigDecimal("3")));

        assertEquals(0, new BigDecimal("1035").compareTo(order.getPayableAmount()));
    }

    @Test
    @DisplayName("未配置押金的 SKU 不计算（配置缺失 SKU002）")
    void shouldSkipSkuWithoutConfig() {
        Order order = orderWithSkus();
        BigDecimal deposit = calculator.applyDeposit(order, Map.of("SKU001", new BigDecimal("2")));

        assertEquals(0, new BigDecimal("20").compareTo(deposit)); // 只有 SKU001：10×2=20
    }

    @Test
    @DisplayName("空配置不改变金额")
    void shouldNoOpWithoutConfig() {
        Order order = orderWithSkus();
        calculator.applyDeposit(order, Map.of());
        assertEquals(0, BigDecimal.ZERO.compareTo(order.getDepositAmount()));
        assertEquals(0, new BigDecimal("1000").compareTo(order.getPayableAmount()));
    }
}
