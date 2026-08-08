package com.demetrius.tribunal.marketing.application.service;

import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.marketing.application.dto.PromotionCalculateRequest;
import com.demetrius.tribunal.marketing.application.dto.PromotionCalculateResponse;
import com.demetrius.tribunal.marketing.domain.model.DepositResult;
import com.demetrius.tribunal.marketing.domain.model.DepositRule;
import com.demetrius.tribunal.marketing.domain.model.PromotionContext;
import com.demetrius.tribunal.marketing.domain.model.PromotionResult;
import com.demetrius.tribunal.marketing.domain.model.PriceRule;
import com.demetrius.tribunal.marketing.domain.model.PriceRuleId;
import com.demetrius.tribunal.marketing.domain.model.SkuItem;
import com.demetrius.tribunal.marketing.domain.repository.DepositRuleRepository;
import com.demetrius.tribunal.marketing.domain.repository.PriceRuleRepository;
import com.demetrius.tribunal.marketing.domain.repository.PromotionRuleRepository;
import com.demetrius.tribunal.marketing.domain.service.DepositEngine;
import com.demetrius.tribunal.marketing.domain.service.PromotionEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 营销价格应用服务。
 *
 * <p>职责：</p>
 * <ol>
 *   <li>取价：按「客户价 → 客户组价 → 区域价」优先级返回 SKU 售价（F-102）</li>
 *   <li>计价：促销折扣计算 + 押金计算，返回金额变更结果（F-202/F-205）</li>
 * </ol>
 */
@Service
public class MarketingApplicationService {

    private final PriceRuleRepository priceRuleRepository;
    private final PromotionRuleRepository promotionRuleRepository;
    private final DepositRuleRepository depositRuleRepository;
    private final PromotionEngine promotionEngine;
    private final DepositEngine depositEngine;

    public MarketingApplicationService(PriceRuleRepository priceRuleRepository,
                                       PromotionRuleRepository promotionRuleRepository,
                                       DepositRuleRepository depositRuleRepository,
                                       PromotionEngine promotionEngine,
                                       DepositEngine depositEngine) {
        this.priceRuleRepository = priceRuleRepository;
        this.promotionRuleRepository = promotionRuleRepository;
        this.depositRuleRepository = depositRuleRepository;
        this.promotionEngine = promotionEngine;
        this.depositEngine = depositEngine;
    }

    // ===== 取价（F-102）=====

    /**
     * 取价：客户价 → 客户组价 → 区域价 优先级。
     */
    @Transactional(readOnly = true)
    public BigDecimal quotePrice(String skuCode, String customerCode,
                                 String customerGroupId, String areaCode) {
        PriceRule rule = priceRuleRepository.findBySkuAndLevel(skuCode, "CUSTOMER", customerCode)
                .or(() -> priceRuleRepository.findBySkuAndLevel(skuCode, "CUSTOMER_GROUP", customerGroupId))
                .or(() -> priceRuleRepository.findBySkuAndLevel(skuCode, "AREA", areaCode))
                .orElseThrow(() -> new BizException("500001", "未找到SKU价格: " + skuCode));
        return rule.getPrice();
    }

    @Transactional
    public PriceRule upsertPrice(String skuCode, String priceLevel, String priceTarget,
                                 BigDecimal price, String currency) {
        PriceRule rule = new PriceRule(new PriceRuleId(generateId()),
                skuCode, priceLevel, priceTarget, price, currency);
        priceRuleRepository.save(rule);
        return rule;
    }

    // ===== 促销 + 押金联合计算（F-202 + F-205）=====

    /**
     * 促销 + 押金联合计算。
     *
     * <p>供 order-service 在下单/审单时调用，一次返回折扣、赠品、押金及分摊明细。</p>
     *
     * @param request 计算请求（客户上下文 + SKU 明细）
     * @return 促销计算响应
     */
    @Transactional(readOnly = true)
    public PromotionCalculateResponse calculate(PromotionCalculateRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            return emptyResponse();
        }

        // 1. 转换为领域 SkuItem
        List<SkuItem> skus = request.items().stream()
                .map(i -> new SkuItem(i.skuCode(), i.skuName(), i.quantity(), i.price()))
                .toList();

        // 2. 促销计算
        PromotionContext ctx = new PromotionContext(request.customerCode(), request.customerGroupId());
        List<com.demetrius.tribunal.marketing.domain.model.PromotionRule> promoRules =
                promotionRuleRepository.findAllActive();
        PromotionResult promoResult = promotionEngine.calculate(skus, promoRules, ctx);

        // 3. 押金计算
        List<String> skuCodes = request.skuCodes() != null ? request.skuCodes()
                : skus.stream().map(SkuItem::skuCode).toList();
        List<DepositRule> depositRules = depositRuleRepository.findBySkuCodes(skuCodes);
        DepositResult depositResult = depositEngine.calculate(skus, depositRules);

        // 4. 组装响应
        BigDecimal payableAddition = depositResult.totalDeposit().subtract(promoResult.discountAmount());
        List<PromotionCalculateResponse.GiftItemDto> gifts = promoResult.giftItems().stream()
                .map(g -> new PromotionCalculateResponse.GiftItemDto(
                        g.skuCode(), g.skuName(), g.quantity()))
                .collect(Collectors.toList());

        return new PromotionCalculateResponse(
                promoResult.discountAmount(),
                depositResult.totalDeposit(),
                payableAddition,
                promoResult.appliedRuleIds(),
                gifts,
                promoResult.skuDiscountBreakdown(),
                depositResult.breakdown());
    }

    private static PromotionCalculateResponse emptyResponse() {
        return new PromotionCalculateResponse(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(), List.of(), Map.of(), Map.of());
    }

    private String generateId() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
}
