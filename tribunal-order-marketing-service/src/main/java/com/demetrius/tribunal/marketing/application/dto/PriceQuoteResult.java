package com.demetrius.tribunal.marketing.application.dto;

import java.math.BigDecimal;

/**
 * 报价结果（marketing-service → order-service）。
 */
public record PriceQuoteResult(
        String skuCode,
        BigDecimal price,
        String currency) {
}
