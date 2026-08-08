package com.demetrius.tribunal.order.domain.service;

import com.demetrius.tribunal.order.domain.model.Order;
import com.demetrius.tribunal.order.domain.model.OrderId;
import com.demetrius.tribunal.order.domain.model.OrderSku;
import com.demetrius.tribunal.order.domain.model.RoutingResult;
import com.demetrius.tribunal.order.domain.model.SkuRequirement;
import com.demetrius.tribunal.order.domain.model.SplitResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * M4：拆单领域服务（蓝图 §3.3）。
 *
 * <p>纯领域逻辑：依据 {@link RoutingResult}（寻源分仓结果）将父单按仓库拆出子单，
 * 并对折扣/押金/税/运费按金额占比分摊（最后一单兜底消除舍入误差）。</p>
 *
 * <p>调用前提：父单已通过寻源，且 {@code routingResult.needsSplit()} 为 true。</p>
 */
@org.springframework.stereotype.Service
public class OrderSplitService {

    private static final int MONEY_SCALE = 2;

    /**
     * 执行拆单。
     *
     * @param parent       父单（CONFIRMED → 进入拆单）
     * @param routingResult 寻源分仓结果
     * @return 拆单结果（父单 + 子单列表）；父单状态迁移至 SPLITTED
     */
    public SplitResult split(Order parent, RoutingResult routingResult) {
        if (parent == null) {
            throw new IllegalArgumentException("父单不能为空");
        }
        if (routingResult == null || !routingResult.needsSplit()) {
            throw new IllegalArgumentException("寻源结果无需拆单");
        }

        // 1. 父单进入拆单中 → 已拆单
        parent.markSplitting();
        parent.completeSplit();

        // 2. 按仓库构建子单
        Map<String, List<OrderSku>> warehouseSkus = groupSkusByWarehouse(parent, routingResult);
        List<String> warehouseIds = new ArrayList<>(warehouseSkus.keySet());
        BigDecimal parentTotal = parent.getTotalAmount();

        List<Order> children = new ArrayList<>(warehouseIds.size());
        // 累计已分摊金额（用于最后一单兜底）
        BigDecimal accDiscount = BigDecimal.ZERO;
        BigDecimal accPool = BigDecimal.ZERO;
        BigDecimal accDeposit = BigDecimal.ZERO;
        BigDecimal accTax = BigDecimal.ZERO;
        BigDecimal accShipping = BigDecimal.ZERO;

        for (int i = 0; i < warehouseIds.size(); i++) {
            String warehouseId = warehouseIds.get(i);
            List<OrderSku> childSkus = warehouseSkus.get(warehouseId);
            boolean isLast = (i == warehouseIds.size() - 1);

            BigDecimal[] prorated;
            if (isLast) {
                // 最后一单：兜底，承担所有舍入差额，保证子单之和 = 父单
                prorated = new BigDecimal[]{
                        parent.getDiscountAmount().subtract(accDiscount),
                        parent.getDiscountPoolDeduction().subtract(accPool),
                        parent.getDepositAmount().subtract(accDeposit),
                        parent.getTaxAmount().subtract(accTax),
                        parent.getShippingFee().subtract(accShipping)
                };
            } else {
                prorated = prorate(childSkus, parentTotal, parent,
                        accDiscount, accPool, accDeposit, accTax, accShipping);
            }

            accDiscount = accDiscount.add(prorated[0]);
            accPool = accPool.add(prorated[1]);
            accDeposit = accDeposit.add(prorated[2]);
            accTax = accTax.add(prorated[3]);
            accShipping = accShipping.add(prorated[4]);

            Order child = Order.createSplitChild(
                    new OrderId(UUID.randomUUID().toString()),
                    parent.getOrderNo() + "-" + String.format("%02d", i + 1),
                    parent, childSkus,
                    prorated[0], prorated[1], prorated[2], prorated[3], prorated[4]);
            children.add(child);
        }

        return new SplitResult(parent, children);
    }

    /**
     * 将父单的 SKU 按寻源仓库分组（绑定 warehouseId）。
     */
    private Map<String, List<OrderSku>> groupSkusByWarehouse(Order parent, RoutingResult routingResult) {
        // 父单 SKU 按 skuCode 索引，便于按需求匹配
        Map<String, OrderSku> parentSkuByCode = new LinkedHashMap<>();
        for (OrderSku s : parent.getSkus()) {
            parentSkuByCode.put(s.getSkuCode(), s);
        }

        Map<String, List<OrderSku>> warehouseSkus = new LinkedHashMap<>();
        for (Map.Entry<String, List<SkuRequirement>> entry : routingResult.assignments().entrySet()) {
            String warehouseId = entry.getKey();
            List<OrderSku> childSkus = new ArrayList<>();
            for (SkuRequirement req : entry.getValue()) {
                OrderSku parentSku = parentSkuByCode.get(req.skuCode());
                if (parentSku == null) {
                    throw new IllegalStateException("父单缺少 SKU: " + req.skuCode());
                }
                // 复制明细并绑定仓库
                childSkus.add(parentSku.withWarehouse(warehouseId));
            }
            warehouseSkus.put(warehouseId, childSkus);
        }
        return warehouseSkus;
    }

    /**
     * 按金额占比分摊折扣/押金/税/运费。
     *
     * @return [discount, poolDeduction, deposit, tax, shipping] 本次分摊值
     */
    private BigDecimal[] prorate(List<OrderSku> childSkus, BigDecimal parentTotal, Order parent,
                                 BigDecimal accDiscount, BigDecimal accPool, BigDecimal accDeposit,
                                 BigDecimal accTax, BigDecimal accShipping) {
        BigDecimal childTotal = childSkus.stream()
                .map(OrderSku::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ratio = childTotal.divide(parentTotal, 6, RoundingMode.HALF_UP);

        return new BigDecimal[]{
                parent.getDiscountAmount().multiply(ratio).setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                parent.getDiscountPoolDeduction().multiply(ratio).setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                parent.getDepositAmount().multiply(ratio).setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                parent.getTaxAmount().multiply(ratio).setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                parent.getShippingFee().multiply(ratio).setScale(MONEY_SCALE, RoundingMode.HALF_UP)
        };
    }
}
