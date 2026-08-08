package com.demetrius.tribunal.order.domain.model;

import java.math.BigDecimal;

/**
 * M4：SKU 需求值对象（寻源服务的输入单元）。
 *
 * <p>表示一笔订单中某个 SKU 的发货需求（编码 + 数量），
 * 供 {@code WarehouseRoutingService} 进行仓库寻源匹配。</p>
 *
 * @param skuCode  SKU 编码
 * @param quantity 需求数量
 */
public record SkuRequirement(String skuCode, BigDecimal quantity) {
}
