package com.demetrius.tribunal.order.domain.service;

import com.demetrius.tribunal.order.domain.model.Order;
import com.demetrius.tribunal.order.domain.model.OrderSku;
import com.demetrius.tribunal.order.domain.model.PromotionRule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 促销计算领域服务（F-202，业务文档二节）。
 *
 * <p>规则：促销与 SKU 组挂钩，按购物车行逐行计算折扣金额；
 * 多个促销命中同一 SKU 时取折扣率最大者（不叠加），避免折扣叠加失控。</p>
 */
public class PromotionCalculator {

    /**
     * 计算促销折扣金额并应用到订单。
     *
     * @param order          订单聚合
     * @param rules          促销规则列表（来源：marketing-service，基础版由应用层传入）
     * @param customerId     客户 ID
     * @param customerGroupId 客户组 ID（可空）
     * @return 本次促销折扣金额
     */
    public BigDecimal applyPromotions(Order order, List<PromotionRule> rules,
                                      String customerId, String customerGroupId) {
        if (rules == null || rules.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalDiscount = BigDecimal.ZERO;
        for (OrderSku sku : order.getSkus()) {
            totalDiscount = totalDiscount.add(calcLineDiscount(sku, rules, customerId, customerGroupId));
        }
        // 促销折扣 + 原折扣合并应用（取两者之和，不超总金额由 Order.applyDiscount 兜底）
        BigDecimal combined = order.getDiscountAmount().add(totalDiscount);
        order.applyDiscount(combined);
        return totalDiscount;
    }

    /**
     * 计算单个购物车行的促销折扣 = 行金额 × 命中规则中最大的折扣率。
     */
    private BigDecimal calcLineDiscount(OrderSku sku, List<PromotionRule> rules,
                                        String customerId, String customerGroupId) {
        BigDecimal maxRate = rules.stream()
                .filter(r -> r.applicableTo(customerId, customerGroupId))
                .filter(r -> r.coversSku(sku.getSkuCode()))
                .map(PromotionRule::discountRate)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        if (maxRate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return sku.getAmount().multiply(maxRate).setScale(2, RoundingMode.HALF_UP);
    }
}
