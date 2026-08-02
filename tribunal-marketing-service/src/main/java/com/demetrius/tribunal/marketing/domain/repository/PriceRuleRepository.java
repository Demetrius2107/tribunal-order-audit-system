package com.demetrius.tribunal.marketing.domain.repository;

import com.demetrius.tribunal.marketing.domain.model.PriceRule;

import java.util.Optional;

/**
 * 价格规则仓储接口。
 *
 * <p>TODO（学习任务）：按 SKU + 档位批量查询（一次取价多档位，避免循环查库）。</p>
 */
public interface PriceRuleRepository {

    void save(PriceRule rule);

    Optional<PriceRule> findById(String id);

    /**
     * 按 SKU + 价格档位 + 价格对象编码查询（客户价/客户组价/区域价）。
     */
    Optional<PriceRule> findBySkuAndLevel(String skuCode, String priceLevel, String priceTarget);
}
