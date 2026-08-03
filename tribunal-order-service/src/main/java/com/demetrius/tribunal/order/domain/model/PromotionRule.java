package com.demetrius.tribunal.order.domain.model;

import java.math.BigDecimal;
import java.util.Set;

/**
 * 促销规则（业务文档二节：客户型/客户组型促销 + SKU 组折扣率）。
 *
 * <p>对应 sales_promotion 与 sales_promotion_sku_group：促销作用于组内 SKU，按行计算折扣金额。</p>
 *
 * @param type            促销类型：1=客户型（针对单个经销商）、2=客户组型（针对一组经销商）
 * @param applicableId    适用对象 ID：客户型=客户ID，客户组型=客户组ID
 * @param skuGroup        SKU 组（促销只作用于组内 SKU）
 * @param discountRate    折扣率（如 0.10 = 打九折，即折扣 10%）
 */
public record PromotionRule(
        int type,
        String applicableId,
        Set<String> skuGroup,
        BigDecimal discountRate) {

    /** 客户型促销（针对单个经销商） */
    public static final int TYPE_CUSTOMER = 1;

    /** 客户组型促销（针对一组经销商） */
    public static final int TYPE_GROUP = 2;

    /** 该促销是否适用于指定客户（客户型按客户ID匹配，客户组型按客户组ID匹配） */
    public boolean applicableTo(String customerId, String customerGroupId) {
        if (type == TYPE_CUSTOMER) {
            return applicableId.equals(customerId);
        }
        if (type == TYPE_GROUP) {
            return applicableId.equals(customerGroupId);
        }
        return false;
    }

    /** 该促销是否作用于指定 SKU */
    public boolean coversSku(String skuCode) {
        return skuGroup != null && skuGroup.contains(skuCode);
    }
}
