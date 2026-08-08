package com.demetrius.tribunal.order.domain.model;

/**
 * 售后类型。
 */
public enum AfterSaleType {

    /** 退货退款：客户寄回商品，确认入库后退款 */
    RETURN_REFUND("退货退款"),

    /** 仅退款：无需退回商品（如质量问题小额补偿、漏发） */
    REFUND_ONLY("仅退款");

    private final String desc;

    AfterSaleType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
