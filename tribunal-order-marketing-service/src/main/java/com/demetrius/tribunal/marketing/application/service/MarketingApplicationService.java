package com.demetrius.tribunal.marketing.application.service;

import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.marketing.domain.model.PriceRule;
import com.demetrius.tribunal.marketing.domain.model.PriceRuleId;
import com.demetrius.tribunal.marketing.domain.repository.PriceRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 营销价格应用服务（取价/计价用例）。
 *
 * <p>对应需求：F-102（价格体系）、F-202（促销计算）、F-205（押金）。</p>
 *
 * <p>职责：</p>
 * <ol>
 *   <li>取价：按「客户价 → 客户组价 → 区域价」优先级返回 SKU 售价</li>
 *   <li>计价：订单金额 → 促销折扣 → 押金 → 应付金额（骨架先做取价，计价留 TODO）</li>
 * </ol>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>促销/押金规则仓储与计算引擎（对照促销计算引擎，骨架未实现）</li>
 *   <li>折扣池余额查询/抵扣（F-203/F-204）</li>
 *   <li>价格缓存（Redis，防缓存穿透/雪崩）</li>
 * </ul>
 */
@Service
public class MarketingApplicationService {

    private final PriceRuleRepository priceRuleRepository;

    public MarketingApplicationService(PriceRuleRepository priceRuleRepository) {
        this.priceRuleRepository = priceRuleRepository;
    }

    /**
     * 取价：客户价 → 客户组价 → 区域价 优先级。
     *
     * @param skuCode         SKU 编码
     * @param customerCode    客户编码（客户价优先）
     * @param customerGroupId 客户组（客户组价）
     * @param areaCode        区域（区域价兜底）
     * @return 售价
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

    /**
     * 新增/更新价格规则（上游主数据推送入口）。
     */
    @Transactional
    public PriceRule upsertPrice(String skuCode, String priceLevel, String priceTarget,
                                 BigDecimal price, String currency) {
        PriceRule rule = new PriceRule(new PriceRuleId(generateId()),
                skuCode, priceLevel, priceTarget, price, currency);
        priceRuleRepository.save(rule);
        return rule;
    }

    private String generateId() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
}
