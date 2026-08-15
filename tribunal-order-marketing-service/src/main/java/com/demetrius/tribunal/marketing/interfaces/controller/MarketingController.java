package com.demetrius.tribunal.marketing.interfaces.controller;

import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.marketing.application.dto.PriceQuoteResult;
import com.demetrius.tribunal.marketing.application.dto.PromotionCalculateRequest;
import com.demetrius.tribunal.marketing.application.dto.PromotionCalculateResponse;
import com.demetrius.tribunal.marketing.application.dto.PromotionRuleResult;
import com.demetrius.tribunal.marketing.application.service.MarketingApplicationService;
import com.demetrius.tribunal.marketing.application.service.PromotionRuleApplicationService;
import com.demetrius.tribunal.marketing.domain.model.PromotionTargetType;
import com.demetrius.tribunal.marketing.domain.model.PromotionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 营销接口层（REST，供订单服务取价/计价）。
 *
 * <p>对应需求：F-102（价格体系）、F-202（促销引擎）、F-205（押金引擎）。</p>
 */
@RestController
@RequestMapping("/api/marketing")
public class MarketingController {

    private final MarketingApplicationService marketingApplicationService;

    private final PromotionRuleApplicationService promotionRuleApplicationService;

    public MarketingController(MarketingApplicationService marketingApplicationService,
                               PromotionRuleApplicationService promotionRuleApplicationService) {
        this.marketingApplicationService = marketingApplicationService;
        this.promotionRuleApplicationService = promotionRuleApplicationService;
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

    // ===== 促销规则配置化（F-201：运营配置，配置即生效）=====

    /**
     * 创建促销规则（草稿态，不参与引擎计算）：POST /api/marketing/promo-rules
     */
    @PostMapping("/promo-rules")
    public ApiResponse<PromotionRuleResult> createRule(@Valid @RequestBody CreateRuleRequest request) {
        return ApiResponse.ok(promotionRuleApplicationService.createRule(
                request.name(), request.type(), request.targetType(), request.targetValue(),
                request.threshold(), request.discountRate(), request.reductionAmount(),
                request.halfPriceRate(), request.applicableSkuCode(),
                request.giftSkuCode(), request.giftSkuName(), request.giftQuantity(),
                request.exclusive() != null && request.exclusive(), request.priority() == null ? 0 : request.priority(),
                request.startTime(), request.endTime()));
    }

    /**
     * 上线促销规则（引擎立即可命中）：POST /api/marketing/promo-rules/{ruleNo}/activate
     */
    @PostMapping("/promo-rules/{ruleNo}/activate")
    public ApiResponse<PromotionRuleResult> activate(@PathVariable String ruleNo) {
        return ApiResponse.ok(promotionRuleApplicationService.activate(ruleNo));
    }

    /**
     * 停用促销规则（引擎立即不再命中）：POST /api/marketing/promo-rules/{ruleNo}/deactivate
     */
    @PostMapping("/promo-rules/{ruleNo}/deactivate")
    public ApiResponse<PromotionRuleResult> deactivate(@PathVariable String ruleNo) {
        return ApiResponse.ok(promotionRuleApplicationService.deactivate(ruleNo));
    }

    /**
     * 查询促销规则：GET /api/marketing/promo-rules/{ruleNo}
     */
    @GetMapping("/promo-rules/{ruleNo}")
    public ApiResponse<PromotionRuleResult> getRule(@PathVariable String ruleNo) {
        return ApiResponse.ok(promotionRuleApplicationService.getRule(ruleNo));
    }

    /** 创建促销规则请求体。 */
    public record CreateRuleRequest(
            @NotBlank String name,
            @NotNull PromotionType type,
            @NotNull PromotionTargetType targetType,
            String targetValue,
            BigDecimal threshold,
            BigDecimal discountRate,
            BigDecimal reductionAmount,
            BigDecimal halfPriceRate,
            String applicableSkuCode,
            String giftSkuCode,
            String giftSkuName,
            BigDecimal giftQuantity,
            Boolean exclusive,
            Integer priority,
            LocalDateTime startTime,
            LocalDateTime endTime) {
    }
}
