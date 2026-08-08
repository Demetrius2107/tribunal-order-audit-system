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

    private BigDecimal price;

    private BigDecimal amount;

    /** M4：寻源仓库 ID（拆单时由寻源服务绑定；未寻源时为 null） */
    private String warehouseId;

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
        this.amount = quantity.multiply(price);
    }

    /**
     * 重新定价（审单时以 marketing 取价覆盖，F-306 审单前重新计价）。
     */
    public void reprice(BigDecimal newPrice) {
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("SKU单价不能为负: " + skuCode);
        }
        this.price = newPrice;
        this.amount = quantity.multiply(newPrice);
    }

    /**
     * M4：绑定寻源仓库（拆单时由寻源服务调用）。
     *
     * @param warehouseId 仓库 ID
     */
    public void assignWarehouse(String warehouseId) {
        if (warehouseId == null || warehouseId.isBlank()) {
            throw new IllegalArgumentException("仓库ID不能为空: " + skuCode);
        }
        this.warehouseId = warehouseId;
    }

    /**
     * M4：复制当前明细并绑定仓库（用于拆单时生成子单明细）。
     */
    public OrderSku withWarehouse(String warehouseId) {
        OrderSku copy = new OrderSku(skuCode, skuName, quantity, price);
        copy.assignWarehouse(warehouseId);
        return copy;
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

    /** M4：寻源仓库 ID（未寻源时为 null） */
    public String getWarehouseId() {
        return warehouseId;
    }
}
