package com.demetrius.tribunal.order.domain.service;

import com.demetrius.tribunal.order.domain.model.RoutingResult;
import com.demetrius.tribunal.order.domain.model.SkuRequirement;
import com.demetrius.tribunal.order.domain.model.WarehouseStock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M4 仓储寻源领域服务单元测试（蓝图 §6.2 验收清单）。
 *
 * <p>覆盖场景：单仓命中、多仓命中、缺货异常、库存充足优先选最大仓。</p>
 */
class WarehouseRoutingServiceTest {

    private final WarehouseRoutingService service = new WarehouseRoutingService();

    @Test
    @DisplayName("单仓命中：所有 SKU 在同一仓库有足够库存 → 不需拆单")
    void shouldRouteToSingleWarehouse() {
        List<SkuRequirement> reqs = List.of(
                new SkuRequirement("SKU001", new BigDecimal("10")),
                new SkuRequirement("SKU002", new BigDecimal("5")));
        List<WarehouseStock> stocks = List.of(
                new WarehouseStock("WH-A", "SKU001", new BigDecimal("100")),
                new WarehouseStock("WH-A", "SKU002", new BigDecimal("50")));

        RoutingResult result = service.route(reqs, stocks);

        assertEquals(1, result.warehouseCount());
        assertFalse(result.needsSplit());
        assertEquals(2, result.assignments().get("WH-A").size());
    }

    @Test
    @DisplayName("多仓命中：不同 SKU 分布在不同仓库 → 需拆单")
    void shouldSplitAcrossMultipleWarehouses() {
        List<SkuRequirement> reqs = List.of(
                new SkuRequirement("SKU001", new BigDecimal("10")),
                new SkuRequirement("SKU002", new BigDecimal("5")));
        List<WarehouseStock> stocks = List.of(
                new WarehouseStock("WH-A", "SKU001", new BigDecimal("100")),
                new WarehouseStock("WH-B", "SKU002", new BigDecimal("50")));

        RoutingResult result = service.route(reqs, stocks);

        assertEquals(2, result.warehouseCount());
        assertTrue(result.needsSplit());
        assertEquals(1, result.assignments().get("WH-A").size());
        assertEquals(1, result.assignments().get("WH-B").size());
    }

    @Test
    @DisplayName("缺货异常：某 SKU 在所有仓库均无足够库存 → 抛 InsufficientStockException")
    void shouldThrowWhenInsufficientStock() {
        List<SkuRequirement> reqs = List.of(
                new SkuRequirement("SKU001", new BigDecimal("200")));
        List<WarehouseStock> stocks = List.of(
                new WarehouseStock("WH-A", "SKU001", new BigDecimal("100")),
                new WarehouseStock("WH-B", "SKU001", new BigDecimal("50")));

        InsufficientStockException ex = assertThrows(InsufficientStockException.class,
                () -> service.route(reqs, stocks));
        assertTrue(ex.getMessage().contains("SKU001"));
    }

    @Test
    @DisplayName("库存充足时优先选库存最大的仓库")
    void shouldPreferWarehouseWithMoreStock() {
        List<SkuRequirement> reqs = List.of(
                new SkuRequirement("SKU001", new BigDecimal("10")));
        List<WarehouseStock> stocks = List.of(
                new WarehouseStock("WH-A", "SKU001", new BigDecimal("30")),
                new WarehouseStock("WH-B", "SKU001", new BigDecimal("100")));

        RoutingResult result = service.route(reqs, stocks);

        // 两个仓库都满足，选库存最大的 WH-B
        assertEquals(1, result.warehouseCount());
        assertTrue(result.assignments().containsKey("WH-B"));
    }

    @Test
    @DisplayName("恰好满足：需求量 = 库存量（边界，可发货）")
    void shouldRouteWhenExactMatch() {
        List<SkuRequirement> reqs = List.of(
                new SkuRequirement("SKU001", new BigDecimal("50")));
        List<WarehouseStock> stocks = List.of(
                new WarehouseStock("WH-A", "SKU001", new BigDecimal("50")));

        RoutingResult result = service.route(reqs, stocks);

        assertEquals(1, result.warehouseCount());
        assertEquals("WH-A", result.assignments().keySet().iterator().next());
    }

    @Test
    @DisplayName("参数校验：空需求列表抛异常")
    void shouldRejectEmptyRequirements() {
        assertThrows(IllegalArgumentException.class,
                () -> service.route(List.of(), List.of(new WarehouseStock("WH-A", "SKU001", BigDecimal.TEN))));
    }

    @Test
    @DisplayName("参数校验：空库存列表抛异常")
    void shouldRejectEmptyStocks() {
        assertThrows(IllegalArgumentException.class,
                () -> service.route(List.of(new SkuRequirement("SKU001", BigDecimal.ONE)), List.of()));
    }
}
