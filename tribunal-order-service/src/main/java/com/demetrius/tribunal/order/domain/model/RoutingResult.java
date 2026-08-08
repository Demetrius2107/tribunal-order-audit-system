package com.demetrius.tribunal.order.domain.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * M4：寻源结果值对象（按仓库分组的 SKU 需求集合）。
 *
 * <p>由 {@code WarehouseRoutingService} 产出，每个 key（仓库 ID）对应一笔子单的发货明细。
 * 调用方可据此判断：单仓（无需拆单）还是多仓（需拆单）。</p>
 *
 * <p>不可变。通过 {@link #builder()} 构造。</p>
 */
public final class RoutingResult {

    /** 仓库 → 该仓库需发货的 SKU 需求列表（保持插入顺序，便于稳定的拆单结果） */
    private final Map<String, List<SkuRequirement>> warehouseAssignments;

    private RoutingResult(Map<String, List<SkuRequirement>> warehouseAssignments) {
        this.warehouseAssignments = Collections.unmodifiableMap(new LinkedHashMap<>(warehouseAssignments));
    }

    /**
     * 参与寻源的仓库数量。
     *
     * @return 仓库数；0 表示无可用仓库
     */
    public int warehouseCount() {
        return warehouseAssignments.size();
    }

    /**
     * 是否需要拆单（多仓命中）。
     *
     * @return true 表示超过一个仓库参与发货
     */
    public boolean needsSplit() {
        return warehouseCount() > 1;
    }

    /**
     * 获取仓库 → SKU 需求的映射（不可变）。
     */
    public Map<String, List<SkuRequirement>> assignments() {
        return warehouseAssignments;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** 寻源结果构造器 */
    public static final class Builder {
        private final Map<String, List<SkuRequirement>> map = new LinkedHashMap<>();

        /**
         * 记录某仓库分配到的 SKU 需求。
         *
         * @param warehouseId  仓库 ID
         * @param requirements 该仓库的 SKU 需求列表
         */
        public Builder assign(String warehouseId, List<SkuRequirement> requirements) {
            map.put(warehouseId, List.copyOf(requirements));
            return this;
        }

        public RoutingResult build() {
            return new RoutingResult(map);
        }
    }
}
