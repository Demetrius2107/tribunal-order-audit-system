package com.demetrius.tribunal.marketing.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.marketing.domain.model.PromotionRule;
import com.demetrius.tribunal.marketing.domain.model.PromotionTargetType;
import com.demetrius.tribunal.marketing.domain.model.PromotionType;
import com.demetrius.tribunal.marketing.domain.repository.PromotionRuleRepository;
import com.demetrius.tribunal.marketing.infrastructure.mapper.PromotionRuleMapper;
import com.demetrius.tribunal.marketing.infrastructure.model.PromotionRulePo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 促销规则仓储实现（MyBatis-Plus）。
 */
@Repository
public class PromotionRuleRepositoryImpl implements PromotionRuleRepository {

    private final PromotionRuleMapper mapper;

    public PromotionRuleRepositoryImpl(PromotionRuleMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(PromotionRule rule) {
        PromotionRulePo po = toPo(rule);
        PromotionRulePo exist = mapper.selectById(rule.getId());
        if (exist == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
    }

    @Override
    public Optional<PromotionRule> findByRuleNo(String ruleNo) {
        PromotionRulePo po = mapper.selectOne(
                new LambdaQueryWrapper<PromotionRulePo>().eq(PromotionRulePo::getRuleNo, ruleNo));
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public List<PromotionRule> findAllActive() {
        return mapper.selectList(
                        new LambdaQueryWrapper<PromotionRulePo>()
                                .eq(PromotionRulePo::getActive, true))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<PromotionRule> findByApplicableSku(String skuCode) {
        return mapper.selectList(
                        new LambdaQueryWrapper<PromotionRulePo>()
                                .eq(PromotionRulePo::getActive, true)
                                .and(w -> w.isNull(PromotionRulePo::getApplicableSkuCode)
                                        .or().eq(PromotionRulePo::getApplicableSkuCode, skuCode)))
                .stream().map(this::toDomain).toList();
    }

    // ---------- 转换 ----------

    private PromotionRule toDomain(PromotionRulePo po) {
        return new PromotionRule(
                po.getId(), po.getRuleNo(), po.getName(),
                PromotionType.valueOf(po.getType()),
                PromotionTargetType.valueOf(po.getTargetType()),
                po.getTargetValue(),
                po.getThreshold(), po.getDiscountRate(), po.getReductionAmount(),
                po.getHalfPriceRate(), po.getApplicableSkuCode(),
                po.getGiftSkuCode(), po.getGiftSkuName(), po.getGiftQuantity(),
                Boolean.TRUE.equals(po.getExclusive()),
                po.getPriority() == null ? 0 : po.getPriority(),
                Boolean.TRUE.equals(po.getActive()),
                po.getStartTime(), po.getEndTime());
    }

    private PromotionRulePo toPo(PromotionRule r) {
        PromotionRulePo po = new PromotionRulePo();
        po.setId(r.getId());
        po.setRuleNo(r.getRuleNo());
        po.setName(r.getName());
        po.setType(r.getType().name());
        po.setTargetType(r.getTargetType().name());
        po.setTargetValue(r.getTargetValue());
        po.setThreshold(r.getThreshold());
        po.setDiscountRate(r.getDiscountRate());
        po.setReductionAmount(r.getReductionAmount());
        po.setHalfPriceRate(r.getHalfPriceRate());
        po.setApplicableSkuCode(r.getApplicableSkuCode());
        po.setGiftSkuCode(r.getGiftSkuCode());
        po.setGiftSkuName(r.getGiftSkuName());
        po.setGiftQuantity(r.getGiftQuantity());
        po.setExclusive(r.isExclusive());
        po.setPriority(r.getPriority());
        po.setActive(r.isActive());
        po.setStartTime(r.getStartTime());
        po.setEndTime(r.getEndTime());
        return po;
    }
}
