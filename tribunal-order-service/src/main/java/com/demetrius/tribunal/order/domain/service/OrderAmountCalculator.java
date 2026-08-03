package com.demetrius.tribunal.order.domain.service;

import com.demetrius.tribunal.order.domain.model.Order;
import com.demetrius.tribunal.order.domain.model.OrderSku;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 订单金额计算领域服务（★金额规则集中地）。
 *
 * <p>对应 F-202（促销计算）/F-102（价格体系）：金额计算规则内聚在此，
 * 不在 Order 聚合或应用服务里散落 BigDecimal 运算。</p>
 *
 * <p>当前规则（基础版）：</p>
 * <ul>
 *   <li>总金额 = Σ(明细数量 × 单价)</li>
 *   <li>应付金额 = 总金额 - 折扣金额（折扣/促销/运费/税后续按规则引擎扩展）</li>
 * </ul>
 */
public class OrderAmountCalculator {

    /**
     * 计算订单总金额 = Σ(明细数量 × 单价)。
     */
    public BigDecimal totalAmount(List<OrderSku> skus) {
        return skus.stream()
                .map(OrderSku::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 计算应付金额 = 总金额 - 折扣金额。
     *
     * <p>折扣金额来自促销/优惠规则（F-202），当前骨架为 0，后续接入营销计算。</p>
     */
    public BigDecimal payableAmount(BigDecimal totalAmount, BigDecimal discountAmount) {
        return totalAmount.subtract(discountAmount == null ? BigDecimal.ZERO : discountAmount);
    }

    /**
     * 按新价格表重新计价：skuCode → 新单价，覆盖明细价格并重算总金额/应付金额。
     *
     * <p>审单时以 marketing-service 取价结果为准（F-306 审单前重新计价），
     * 防止前端/下单时传入的价格与定价体系不一致。</p>
     *
     * @param order       订单聚合
     * @param priceBySku  SKU编码 → 新单价；未出现的 SKU 保持原价
     */
    public void reprice(Order order, Map<String, BigDecimal> priceBySku) {
        for (OrderSku sku : order.getSkus()) {
            BigDecimal newPrice = priceBySku.get(sku.getSkuCode());
            if (newPrice != null) {
                sku.reprice(newPrice);
            }
        }
        order.recalculateAmounts();
    }
}
