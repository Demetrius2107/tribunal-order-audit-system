package com.demetrius.tribunal.marketing.domain.model;

/**
 * 促销适用对象类型（决定一条规则对哪些客户生效）。
 */
public enum PromotionTargetType {
    /** 指定客户：targetValue = 客户编码 */
    CUSTOMER,
    /** 指定客户组：targetValue = 客户组编码 */
    CUSTOMER_GROUP,
    /** 全部客户：targetValue 忽略 */
    ALL
}
