package com.demetrius.tribunal.marketing.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.marketing.domain.model.PriceRule;
import com.demetrius.tribunal.marketing.domain.model.PriceRuleId;
import com.demetrius.tribunal.marketing.domain.repository.PriceRuleRepository;
import com.demetrius.tribunal.marketing.infrastructure.mapper.PriceRuleMapper;
import com.demetrius.tribunal.marketing.infrastructure.model.PriceRulePo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 价格规则仓储实现（MyBatis-Plus）。
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>批量取价（一次查多档位，避免循环查库）</li>
 *   <li>Redis 价格缓存（防缓存穿透/雪崩）</li>
 * </ul>
 */
@Repository
public class PriceRuleRepositoryImpl implements PriceRuleRepository {

    private final PriceRuleMapper priceRuleMapper;

    public PriceRuleRepositoryImpl(PriceRuleMapper priceRuleMapper) {
        this.priceRuleMapper = priceRuleMapper;
    }

    @Override
    public void save(PriceRule rule) {
        PriceRulePo po = toPo(rule);
        PriceRulePo exist = priceRuleMapper.selectOne(
                new LambdaQueryWrapper<PriceRulePo>()
                        .eq(PriceRulePo::getSkuCode, rule.getSkuCode())
                        .eq(PriceRulePo::getPriceLevel, rule.getPriceLevel())
                        .eq(PriceRulePo::getPriceTarget, rule.getPriceTarget()));
        if (exist == null) {
            priceRuleMapper.insert(po);
        } else {
            po.setId(exist.getId());
            priceRuleMapper.updateById(po);
        }
    }

    @Override
    public Optional<PriceRule> findById(String id) {
        PriceRulePo po = priceRuleMapper.selectById(id);
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public Optional<PriceRule> findBySkuAndLevel(String skuCode, String priceLevel, String priceTarget) {
        if (priceTarget == null || priceTarget.isBlank()) {
            return Optional.empty();
        }
        PriceRulePo po = priceRuleMapper.selectOne(
                new LambdaQueryWrapper<PriceRulePo>()
                        .eq(PriceRulePo::getSkuCode, skuCode)
                        .eq(PriceRulePo::getPriceLevel, priceLevel)
                        .eq(PriceRulePo::getPriceTarget, priceTarget));
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    // ---------- 转换方法（PO ↔ 领域对象） ----------

    private PriceRule toDomain(PriceRulePo po) {
        return new PriceRule(new PriceRuleId(po.getId()), po.getSkuCode(),
                po.getPriceLevel(), po.getPriceTarget(), po.getPrice(), po.getCurrency());
    }

    private PriceRulePo toPo(PriceRule rule) {
        PriceRulePo po = new PriceRulePo();
        po.setId(rule.getId().value());
        po.setSkuCode(rule.getSkuCode());
        po.setPriceLevel(rule.getPriceLevel());
        po.setPriceTarget(rule.getPriceTarget());
        po.setPrice(rule.getPrice());
        po.setCurrency(rule.getCurrency());
        return po;
    }
}
