package com.demetrius.tribunal.order.client;

import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.order.client.fallback.MarketingFeignFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/**
 * 营销价格服务（marketing-service）的 Feign 客户端。
 *
 * <p>对应需求：F-102（价格体系）、F-202（促销引擎）、F-205（押金引擎）。</p>
 */
@FeignClient(name = "tribunal-order-marketing-service",
        fallbackFactory = MarketingFeignFallbackFactory.class)
public interface MarketingFeignClient {

    /**
     * 取价：GET /api/marketing/price
     */
    @GetMapping("/api/marketing/price")
    ApiResponse<PriceQuoteResult> quotePrice(@RequestParam("skuCode") String skuCode,
                                             @RequestParam(value = "customerCode", required = false) String customerCode,
                                             @RequestParam(value = "customerGroupId", required = false) String customerGroupId,
                                             @RequestParam(value = "areaCode", required = false) String areaCode);

    /**
     * 促销 + 押金联合计算：POST /api/marketing/calculate
     */
    @PostMapping("/api/marketing/calculate")
    ApiResponse<PromotionCalculateResponse> calculate(@RequestBody PromotionCalculateRequest request);
}
