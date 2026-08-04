package com.demetrius.tribunal.order.domain.service;

import com.demetrius.tribunal.order.domain.model.Order;
import com.demetrius.tribunal.order.domain.model.OrderSku;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 押金计算领域服务（F-205，业务文档四节）。
 *
 * <p>规则：购物车行按 SKU 查找客户-SKU 押金配置（customer_sku_deposit），
 * 押金金额 = Σ(明细数量 × 该 SKU 押金单价)，然后应用到订单（参与应付金额汇总）。</p>
 */
public class DepositCalculator {

    /**
     * 按 SKU-客户押金配置计算押金并应用到订单。
     *
     * @param order            订单聚合
     * @param unitDepositBySku SKU编码 → 押金单价（来源：customer_sku_deposit 配置，基础版由应用层传入）
     * @return 本次计算的押金总额
     */
    public BigDecimal applyDeposit(Order order, Map<String, BigDecimal> unitDepositBySku) {
        if (unitDepositBySku == null || unitDepositBySku.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalDeposit = BigDecimal.ZERO;
        for (OrderSku sku : order.getSkus()) {
            BigDecimal unitDeposit = unitDepositBySku.get(sku.getSkuCode());
            if (unitDeposit != null && unitDeposit.compareTo(BigDecimal.ZERO) > 0) {
                totalDeposit = totalDeposit.add(sku.getQuantity().multiply(unitDeposit));
            }
        }
        if (totalDeposit.compareTo(BigDecimal.ZERO) > 0) {
            order.applyDeposit(totalDeposit);
        }
        return totalDeposit;
    }
}
