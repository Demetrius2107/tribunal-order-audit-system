package com.demetrius.tribunal.order.application.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 下单应用层入参（应用层 DTO）。
 *
 * <p>对照旧项目：{@code OrderDto} / {@code OrderSkuDomain}（下单请求体）。</p>
 *
 * <p>TODO（学习任务）：对照旧项目下单请求，补充：</p>
 * <ul>
 *   <li>送货地址（shippingCode / 收货人 / 电话）</li>
 *   <li>运输方式（transportModel）</li>
 *   <li>回瓶信息（ReturnBottle，啤酒行业特有）</li>
 *   <li>订单类型（普通/冬储，对照 OrderConstants.WINTER_ORDER_TYPE）</li>
 * </ul>
 */
public record OrderCreateCommand(
        String customerId,
        List<SkuItem> skus) {

    public record SkuItem(
            String skuCode,
            String skuName,
            BigDecimal quantity,
            BigDecimal price) {
    }
}
