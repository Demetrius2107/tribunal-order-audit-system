package com.demetrius.tribunal.marketing.domain.repository;

import com.demetrius.tribunal.marketing.domain.model.DepositRule;

import java.util.List;

/**
 * 押金规则仓储接口。
 */
public interface DepositRuleRepository {

    void save(DepositRule rule);

    /**
     * 按 SKU 编码列表批量查询押金规则。
     */
    List<DepositRule> findBySkuCodes(List<String> skuCodes);

    /**
     * 查询全部启用中的押金规则。
     */
    List<DepositRule> findAllActive();
}
