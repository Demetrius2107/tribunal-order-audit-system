package com.demetrius.tribunal.marketing.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.marketing.domain.model.DepositRule;
import com.demetrius.tribunal.marketing.domain.model.PackagingType;
import com.demetrius.tribunal.marketing.domain.repository.DepositRuleRepository;
import com.demetrius.tribunal.marketing.infrastructure.mapper.DepositRuleMapper;
import com.demetrius.tribunal.marketing.infrastructure.model.DepositRulePo;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 押金规则仓储实现（MyBatis-Plus）。
 */
@Repository
public class DepositRuleRepositoryImpl implements DepositRuleRepository {

    private final DepositRuleMapper mapper;

    public DepositRuleRepositoryImpl(DepositRuleMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(DepositRule rule) {
        DepositRulePo po = toPo(rule);
        DepositRulePo exist = mapper.selectById(rule.getId());
        if (exist == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
    }

    @Override
    public List<DepositRule> findBySkuCodes(List<String> skuCodes) {
        if (skuCodes == null || skuCodes.isEmpty()) {
            return List.of();
        }
        return mapper.selectList(
                        new LambdaQueryWrapper<DepositRulePo>()
                                .eq(DepositRulePo::getActive, true)
                                .in(DepositRulePo::getSkuCode, skuCodes))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<DepositRule> findAllActive() {
        return mapper.selectList(
                        new LambdaQueryWrapper<DepositRulePo>()
                                .eq(DepositRulePo::getActive, true))
                .stream().map(this::toDomain).toList();
    }

    // ---------- 转换 ----------

    private DepositRule toDomain(DepositRulePo po) {
        return new DepositRule(
                po.getId(), po.getSkuCode(),
                PackagingType.valueOf(po.getPackagingType()),
                po.getUnitDeposit(),
                Boolean.TRUE.equals(po.getIncludedInPrice()),
                Boolean.TRUE.equals(po.getActive()));
    }

    private DepositRulePo toPo(DepositRule r) {
        DepositRulePo po = new DepositRulePo();
        po.setId(r.getId());
        po.setSkuCode(r.getSkuCode());
        po.setPackagingType(r.getPackagingType().name());
        po.setUnitDeposit(r.getUnitDeposit());
        po.setIncludedInPrice(r.isIncludedInPrice());
        po.setActive(r.isActive());
        return po;
    }
}
