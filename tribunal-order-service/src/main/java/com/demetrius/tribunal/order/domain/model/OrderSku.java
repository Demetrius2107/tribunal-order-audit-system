package com.demetrius.tribunal.order.domain.model;

import java.math.BigDecimal;

/**
 * 订单明细（聚合内实体）。
 *
 * <p>参照通用做法。</p>
 *
 * <p>聚合内实体没有独立生命周期：不能脱离订单单独存在，只能通过聚合根 Order 的方法增删改。</p>
 *
 * <p>TODO（学习任务）：、押金（deposit）、折扣前金额（discountBeforeAmount）等</li>
 *   <li>补充数量校验：数量必须大于 0；整托校验（啤酒行业特有）</li>
 *   <li>金额计算规则：amount = price * quantity，折扣、押金、税如何参与——这是促销计算的核心</li>
 * </ul>
 */
public class OrderSku {

    private final String skuCode;

    private final String skuName;

    private final BigDecimal quantity;

    private final BigDecimal price;

    private BigDecimal amount;

    public OrderSku(String skuCode, String skuName, BigDecimal quantity, BigDecimal price) {
        if (skuCode == null || skuCode.isBlank()) {
            throw new IllegalArgumentException("SKU编码不能为空");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("SKU数量必须大于0");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("SKU单价不能为负");
        }
        this.skuCode = skuCode;
        this.skuName = skuName;
        this.quantity = quantity;
        this.price = price;
        // TODO（学习任务）：金额 = 数量 * 单价（后续可扩展：押金/折扣/税参与计算）
        this.amount = quantity.multiply(price);
    }

    public String getSkuCode() {
        return skuCode;
    }

    public String getSkuName() {
        return skuName;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
