package com.demetrius.tribunal.marketing.domain.repository;

import com.demetrius.tribunal.marketing.domain.model.PromotionRule;

import java.util.List;

/**
 * 促销规则仓储接口。
 */
public interface PromotionRuleRepository {

    void save(PromotionRule rule);

    /**
     * 查询全部启用中的促销规则（引擎在内存中按上下文/有效期/优先级筛选）。
     */
    List<PromotionRule> findAllActive();

    /**
     * 按适用 SKU 查询促销规则（用于第二件半价等 SKU 级规则快速命中）。
     */
    List<PromotionRule> findByApplicableSku(String skuCode);
}
