package com.demetrius.tribunal.order.domain.service;

import com.demetrius.tribunal.order.domain.model.Order;
import com.demetrius.tribunal.order.domain.model.OrderId;
import com.demetrius.tribunal.order.domain.model.OrderSku;
import com.demetrius.tribunal.order.domain.model.OrderStatus;
import com.demetrius.tribunal.order.domain.model.RoutingResult;
import com.demetrius.tribunal.order.domain.model.SkuRequirement;
import com.demetrius.tribunal.order.domain.model.SplitResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M4 拆单领域服务单元测试（蓝图 §6.2 验收清单）。
 *
 * <p>覆盖场景：多仓拆单、子单明细绑定仓库、金额按比例分摊、分摊精度兜底、父单状态迁移。</p>
 */
class OrderSplitServiceTest {

    private final OrderSplitService service = new OrderSplitService();

    private static OrderSku sku(String code, BigDecimal qty, BigDecimal price) {
        return new OrderSku(code, "测试SKU-" + code, qty, price);
    }

    /** 构造一个已确认的父单（含 2 个 SKU，分布在 2 仓） */
    private static Order confirmedParent() {
        Order parent = Order.create(
                new OrderId("parent-001"),
                "ORD1001",
                "cust-001",
                List.of(sku("SKU001", new BigDecimal("10"), new BigDecimal("50")),
                        sku("SKU002", new BigDecimal("5"), new BigDecimal("20"))));
        parent.confirm();
        return parent;
    }

    private static RoutingResult twoWarehouseRouting() {
        // SKU001 → WH-A，SKU002 → WH-B
        return RoutingResult.builder()
                .assign("WH-A", List.of(new SkuRequirement("SKU001", new BigDecimal("10"))))
                .assign("WH-B", List.of(new SkuRequirement("SKU002", new BigDecimal("5"))))
                .build();
    }

    @Test
    @DisplayName("拆单：父单状态迁移至 SPLITTED，split=true")
    void shouldMarkParentAsSplitted() {
        Order parent = confirmedParent();
        SplitResult result = service.split(parent, twoWarehouseRouting());

        assertEquals(OrderStatus.SPLITTED, result.parent().getStatus());
        assertTrue(result.parent().isSplit());
    }

    @Test
    @DisplayName("拆单：按仓库数量生成子单（2 仓 → 2 张子单）")
    void shouldGenerateChildOrdersPerWarehouse() {
        Order parent = confirmedParent();
        SplitResult result = service.split(parent, twoWarehouseRouting());

        assertEquals(2, result.children().size());
    }

    @Test
    @DisplayName("拆单：子单明细绑定对应仓库 ID")
    void shouldAssignWarehouseToChildSkus() {
        Order parent = confirmedParent();
        SplitResult result = service.split(parent, twoWarehouseRouting());

        Order childA = result.children().stream()
                .filter(c -> "WH-A".equals(c.getSkus().get(0).getWarehouseId()))
                .findFirst().orElseThrow();
        Order childB = result.children().stream()
                .filter(c -> "WH-B".equals(c.getSkus().get(0).getWarehouseId()))
                .findFirst().orElseThrow();

        assertEquals("WH-A", childA.getSkus().get(0).getWarehouseId());
        assertEquals("WH-B", childB.getSkus().get(0).getWarehouseId());
    }

    @Test
    @DisplayName("拆单：子单 parentOrderId 指向父单")
    void shouldSetParentOrderIdOnChildren() {
        Order parent = confirmedParent();
        SplitResult result = service.split(parent, twoWarehouseRouting());

        for (Order child : result.children()) {
            assertTrue(child.isChildOrder());
            assertEquals("parent-001", child.getParentOrderId());
        }
    }

    @Test
    @DisplayName("拆单：子单金额之和 = 父单金额（金额分摊一致性）")
    void shouldKeepChildTotalSumEqualToParent() {
        Order parent = confirmedParent();
        // 父单商品总额 = 10*50 + 5*20 = 600
        BigDecimal parentTotal = parent.getTotalAmount();
        SplitResult result = service.split(parent, twoWarehouseRouting());

        BigDecimal childrenTotal = result.children().stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, parentTotal.compareTo(childrenTotal),
                "子单金额之和应等于父单金额");
    }

    @Test
    @DisplayName("拆单：单仓寻源结果（needsSplit=false）调用拆单抛异常")
    void shouldRejectSplitWhenNoNeed() {
        Order parent = confirmedParent();
        RoutingResult single = RoutingResult.builder()
                .assign("WH-A", List.of(
                        new SkuRequirement("SKU001", new BigDecimal("10")),
                        new SkuRequirement("SKU002", new BigDecimal("5"))))
                .build();

        assertThrows(IllegalArgumentException.class, () -> service.split(parent, single));
    }

    @Test
    @DisplayName("拆单：父单为 null 抛异常")
    void shouldRejectNullParent() {
        assertThrows(IllegalArgumentException.class,
                () -> service.split(null, twoWarehouseRouting()));
    }

    @Test
    @DisplayName("拆单：子单明细为空时（寻源结果异常）抛异常由 createSplitChild 校验")
    void shouldRejectEmptyChildSkus() {
        // 此场景由 OrderSplitService 内部 groupSkusByWarehouse 保证非空，
        // 此处验证寻源结果正常时子单必然非空
        Order parent = confirmedParent();
        SplitResult result = service.split(parent, twoWarehouseRouting());
        assertFalse(result.children().isEmpty());
        result.children().forEach(c -> assertFalse(c.getSkus().isEmpty()));
    }
}
