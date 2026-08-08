package com.demetrius.tribunal.order.domain.service;

import com.demetrius.tribunal.order.domain.model.RoutingResult;
import com.demetrius.tribunal.order.domain.model.SkuRequirement;
import com.demetrius.tribunal.order.domain.model.WarehouseStock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * M4：仓储寻源领域服务（蓝图 §3.2）。
 *
 * <p>纯领域逻辑，不依赖任何 I/O。输入为订单的 SKU 需求集合 + 各仓库的可用库存，
 * 输出为 {@link RoutingResult}（按仓库分组的发货分配方案）。</p>
 *
 * <p><b>寻源规则（贪心，单 SKU 单仓）：</b></p>
 * <ol>
 *   <li>对每个 SKU 需求，筛出可用库存 ≥ 需求数量的仓库候选集</li>
 *   <li>候选集按可用库存降序，取库存最充足的仓库发货</li>
 *   <li>若无任何仓库可满足该 SKU 需求 → 抛出 {@link InsufficientStockException}（缺货）</li>
 *   <li>最后按仓库分组：同一仓库的 SKU 汇总为一张子单的明细</li>
 * </ol>
 *
 * <p>仓库数量决定是否拆单：单仓 → 无需拆单；多仓 → 需拆单。</p>
 */
@org.springframework.stereotype.Service
public class WarehouseRoutingService {

    /**
     * 执行寻源分仓。
     *
     * @param requirements 订单的 SKU 需求列表（不可为空）
     * @param stocks       各仓库的可用库存列表（不可为空）
     * @return 寻源结果（按仓库分组）
     * @throws InsufficientStockException 当某 SKU 在所有仓库均无足够库存时
     */
    public RoutingResult route(List<SkuRequirement> requirements, List<WarehouseStock> stocks) {
        if (requirements == null || requirements.isEmpty()) {
            throw new IllegalArgumentException("SKU需求列表不能为空");
        }
        if (stocks == null || stocks.isEmpty()) {
            throw new IllegalArgumentException("仓库库存列表不能为空");
        }

        // warehouseId → 该仓库分配到的 SKU 需求（保持插入顺序）
        Map<String, List<SkuRequirement>> assignments = new LinkedHashMap<>();

        for (SkuRequirement req : requirements) {
            String warehouseId = selectWarehouse(req, stocks);
            assignments.computeIfAbsent(warehouseId, k -> new ArrayList<>()).add(req);
        }

        RoutingResult.Builder builder = RoutingResult.builder();
        assignments.forEach(builder::assign);
        return builder.build();
    }

    /**
     * 为单个 SKU 需求选择库存最充足且可满足的仓库。
     *
     * @throws InsufficientStockException 无仓库可满足时
     */
    private String selectWarehouse(SkuRequirement req, List<WarehouseStock> stocks) {
        return stocks.stream()
                .filter(s -> s.skuCode().equals(req.skuCode()))
                .filter(s -> s.availableQuantity().compareTo(req.quantity()) >= 0)
                .max(Comparator.comparing(WarehouseStock::availableQuantity))
                .map(WarehouseStock::warehouseId)
                .orElseThrow(() -> new InsufficientStockException(
                        "SKU[" + req.skuCode() + "] 在所有仓库均无足够库存，需求数量=" + req.quantity()));
    }
}
