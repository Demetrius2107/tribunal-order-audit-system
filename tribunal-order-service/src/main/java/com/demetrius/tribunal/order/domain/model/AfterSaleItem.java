package com.demetrius.tribunal.order.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 售后明细（值对象）。
 *
 * <p>记录单个 SKU 的退货数量及退款金额。退款金额按原单中该 SKU 的实付单价计算，
 * 押金退还按该 SKU 占订单总额比例分摊。</p>
 *
 * @param skuCode        SKU 编码
 * @param skuName        SKU 名称
 * @param quantity       退货数量
 * @param refundAmount   退款金额（商品部分）
 * @param depositRefund  押金退还金额
 */
public record AfterSaleItem(String skuCode,
                            String skuName,
                            BigDecimal quantity,
                            BigDecimal refundAmount,
                            BigDecimal depositRefund) {

    /**
     * 按原单明细计算退款金额。
     *
     * @param sku         原单明细
     * @param returnQty   退货数量
     * @param depositRate 押金退还率（0~1，全退=1）
     * @return 售后明细
     */
    public static AfterSaleItem fromOrderSku(OrderSku sku, BigDecimal returnQty, BigDecimal depositRate) {
        if (returnQty == null || returnQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("退货数量必须大于0: " + sku.getSkuCode());
        }
        if (returnQty.compareTo(sku.getQuantity()) > 0) {
            throw new IllegalArgumentException("退货数量超过订单购买数量: " + sku.getSkuCode());
        }
        // 退款金额 = 单价 × 退货数量
        BigDecimal refund = sku.getPrice().multiply(returnQty).setScale(2, RoundingMode.HALF_UP);
        // 押金退还 = 该 SKU 押金 × 退货数量占比 × 退还率（此处简化：单 SKU 押金视为 0，由上层分摊后传入）
        BigDecimal depositRefund = BigDecimal.ZERO;
        return new AfterSaleItem(sku.getSkuCode(), sku.getSkuName(), returnQty, refund, depositRefund);
    }

    /** 明细退款合计 = 商品退款 + 押金退还 */
    public BigDecimal totalRefund() {
        return refundAmount.add(depositRefund);
    }
}
