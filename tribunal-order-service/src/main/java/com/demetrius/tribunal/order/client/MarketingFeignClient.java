package com.demetrius.tribunal.order.client;

import com.demetrius.tribunal.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/**
 * 营销价格服务（marketing-service）的 Feign 客户端。
 *
 * <p>对应需求：F-102（价格体系）——上游"金额"数据源。</p>
 *
 * <p>用途：下单/审单时取价（客户价→客户组价→区域价），替代前端传入价格。</p>
 */
@FeignClient(name = "tribunal-marketing-service",
        url = "${marketing.service.url:http://localhost:8084}")
public interface MarketingFeignClient {

    /**
     * 取价：GET /api/marketing/price
     */
    @GetMapping("/api/marketing/price")
    ApiResponse<PriceQuoteResult> quotePrice(@RequestParam("skuCode") String skuCode,
                                             @RequestParam(value = "customerCode", required = false) String customerCode,
                                             @RequestParam(value = "customerGroupId", required = false) String customerGroupId,
                                             @RequestParam(value = "areaCode", required = false) String areaCode);
}
