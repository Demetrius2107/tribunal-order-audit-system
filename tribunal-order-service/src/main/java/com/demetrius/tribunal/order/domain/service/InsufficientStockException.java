package com.demetrius.tribunal.order.domain.service;

/**
 * M4：库存不足异常（寻源阶段某 SKU 在所有仓库均无足够库存时抛出）。
 *
 * <p>调用方（应用服务）捕获后可决定：整单挂起、回退到待确认、或转人工处理。</p>
 */
public class InsufficientStockException extends RuntimeException {

    /** 缺货的 SKU 编码（便于上层定位） */
    private final String skuCode;

    public InsufficientStockException(String message) {
        super(message);
        // 从 message 中无法稳定解析，skuCode 暂置空；调用方可通过构造器显式传入
        this.skuCode = "";
    }

    public InsufficientStockException(String skuCode, String message) {
        super(message);
        this.skuCode = skuCode;
    }

    public String getSkuCode() {
        return skuCode;
    }
}
