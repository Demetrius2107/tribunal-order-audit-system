package com.demetrius.tribunal.order.application.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 下单应用层入参（应用层 DTO）。
 *
 * <p>。</p>
 *
 * <p>TODO（学习任务）：参照通用做法，补充：</p>
 * <ul>
 *   <li>送货地址（shippingCode / 收货人 / 电话）</li>
 *   <li>运输方式（transportModel）</li>
 *   <li>回瓶信息（ReturnBottle，啤酒行业特有）</li>
 *   <li>订单类型（普通/冬储，参照通用做法</li>
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
