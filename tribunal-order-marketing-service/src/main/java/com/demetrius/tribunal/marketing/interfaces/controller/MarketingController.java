package com.demetrius.tribunal.marketing.interfaces.controller;

import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.marketing.application.dto.PriceQuoteResult;
import com.demetrius.tribunal.marketing.application.service.MarketingApplicationService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 营销价格接口层（REST，供订单服务取价）。
 *
 * <p>对应需求：F-102（价格体系）、F-202（促销计算）。</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>促销计算接口（批量计价）</li>
 *   <li>押金查询接口</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/marketing")
public class MarketingController {

    private final MarketingApplicationService marketingApplicationService;

    public MarketingController(MarketingApplicationService marketingApplicationService) {
        this.marketingApplicationService = marketingApplicationService;
    }

    /**
     * 取价：GET /api/marketing/price?skuCode=..&customerCode=..&customerGroupId=..&areaCode=..
     */
    @GetMapping("/price")
    public ApiResponse<PriceQuoteResult> quotePrice(
            @RequestParam String skuCode,
            @RequestParam(required = false) String customerCode,
            @RequestParam(required = false) String customerGroupId,
            @RequestParam(required = false) String areaCode) {
        BigDecimal price = marketingApplicationService.quotePrice(
                skuCode, customerCode, customerGroupId, areaCode);
        return ApiResponse.ok(new PriceQuoteResult(skuCode, price, "CNY"));
    }

    /**
     * 心跳接口（运维探活）。
     */
    @GetMapping("/heartbeat")
    public ApiResponse<String> heartbeat() {
        return ApiResponse.ok("UP");
    }
}
