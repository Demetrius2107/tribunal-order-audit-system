package com.demetrius.tribunal.order.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 合单明细（值对象）。
 *
 * <p>每条明细记录「来源订单 + 一个 SKU」，创建合单时从成员订单的 OrderSku 展开。
 * 同一个 SKU 来自不同订单会有多条记录（保留订单可追溯性）。</p>
 *
 * @param orderId    来源订单 ID
 * @param orderNo    来源订单编号
 * @param skuCode    SKU 编码
 * @param skuName    SKU 名称
 * @param quantity   数量
 * @param unitAmount 单价
 */
public record MergeOrderItem(
        String orderId,
        String orderNo,
        String skuCode,
        String skuName,
        BigDecimal quantity,
        BigDecimal unitAmount
) {

    public MergeOrderItem {
        Objects.requireNonNull(orderId, "来源订单ID不能为空");
        Objects.requireNonNull(skuCode, "SKU编码不能为空");
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("合单明细数量必须大于0");
        }
        if (unitAmount == null || unitAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("合单明细单价不能为负");
        }
    }

    /** 该明细行的小计金额 = 数量 × 单价 */
    public BigDecimal subTotal() {
        return quantity.multiply(unitAmount);
    }
}
