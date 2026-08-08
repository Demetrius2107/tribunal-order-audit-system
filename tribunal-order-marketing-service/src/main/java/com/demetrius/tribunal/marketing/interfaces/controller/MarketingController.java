package com.demetrius.tribunal.marketing.interfaces.controller;

import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.marketing.application.dto.PriceQuoteResult;
import com.demetrius.tribunal.marketing.application.dto.PromotionCalculateRequest;
import com.demetrius.tribunal.marketing.application.dto.PromotionCalculateResponse;
import com.demetrius.tribunal.marketing.application.service.MarketingApplicationService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 营销接口层（REST，供订单服务取价/计价）。
 *
 * <p>对应需求：F-102（价格体系）、F-202（促销引擎）、F-205（押金引擎）。</p>
 */
@RestController
@RequestMapping("/api/marketing")
public class MarketingController {

    private final MarketingApplicationService marketingApplicationService;

    public MarketingController(MarketingApplicationService marketingApplicationService) {
        this.marketingApplicationService = marketingApplicationService;
    }

    // ===== 取价（F-102）=====

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

    // ===== 促销 + 押金联合计算（F-202 + F-205）=====

    /**
     * 计算促销与押金：POST /api/marketing/calculate
     *
     * <p>order-service 在下单/审单时调用，返回折扣金额、赠品、押金及分摊明细。</p>
     */
    @PostMapping("/calculate")
    public ApiResponse<PromotionCalculateResponse> calculate(
            @RequestBody PromotionCalculateRequest request) {
        return ApiResponse.ok(marketingApplicationService.calculate(request));
    }

    /**
     * 心跳接口（运维探活）。
     */
    @GetMapping("/heartbeat")
    public ApiResponse<String> heartbeat() {
        return ApiResponse.ok("UP");
    }
}
