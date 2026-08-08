package com.demetrius.tribunal.marketing.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.marketing.domain.model.CouponTemplate;
import com.demetrius.tribunal.marketing.domain.model.CouponType;
import com.demetrius.tribunal.marketing.domain.repository.CouponTemplateRepository;
import com.demetrius.tribunal.marketing.infrastructure.mapper.CouponTemplateMapper;
import com.demetrius.tribunal.marketing.infrastructure.model.CouponTemplatePo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 券模板仓储实现。
 */
@Repository
public class CouponTemplateRepositoryImpl implements CouponTemplateRepository {

    private final CouponTemplateMapper mapper;

    public CouponTemplateRepositoryImpl(CouponTemplateMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(CouponTemplate template) {
        CouponTemplatePo po = toPo(template);
        CouponTemplatePo existing = mapper.selectById(po.getId());
        if (existing == null) {
            mapper.insert(po);
        } else {
            // 乐观锁：version 不匹配时影响行数为 0（并发超发/并发修改冲突）
            if (mapper.updateById(po) == 0) {
                throw new BizException("409201", "券模板已被并发修改，请重试");
            }
        }
    }

    @Override
    public Optional<CouponTemplate> findById(String id) {
        CouponTemplatePo po = mapper.selectById(id);
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public Optional<CouponTemplate> findByIdForUpdate(String id) {
        CouponTemplatePo po = mapper.selectOne(
                new LambdaQueryWrapper<CouponTemplatePo>()
                        .eq(CouponTemplatePo::getId, id)
                        .last("FOR UPDATE"));
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public Optional<CouponTemplate> findByTemplateNo(String templateNo) {
        CouponTemplatePo po = mapper.selectOne(
                new LambdaQueryWrapper<CouponTemplatePo>()
                        .eq(CouponTemplatePo::getTemplateNo, templateNo));
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public List<CouponTemplate> findAllActive() {
        return mapper.selectList(
                        new LambdaQueryWrapper<CouponTemplatePo>()
                                .eq(CouponTemplatePo::getActive, true))
                .stream().map(this::toDomain).toList();
    }

    // ---------- 转换 ----------

    private CouponTemplate toDomain(CouponTemplatePo po) {
        return CouponTemplate.restore(
                po.getId(), po.getTemplateNo(), po.getName(),
                CouponType.valueOf(po.getType()),
                po.getThreshold(), po.getDeductionAmount(), po.getDiscountRate(),
                po.getTotalQuota(),
                po.getPerUserLimit() == null ? 1 : po.getPerUserLimit(),
                po.getValidStartTime(), po.getValidEndTime(),
                Boolean.TRUE.equals(po.getActive()),
                po.getIssuedCount() == null ? 0 : po.getIssuedCount(),
                po.getCreateTime(), po.getUpdateTime(),
                po.getVersion() == null ? 0 : po.getVersion());
    }

    private CouponTemplatePo toPo(CouponTemplate t) {
        CouponTemplatePo po = new CouponTemplatePo();
        po.setId(t.getId());
        po.setTemplateNo(t.getTemplateNo());
        po.setName(t.getName());
        po.setType(t.getType().name());
        po.setThreshold(t.getThreshold());
        po.setDeductionAmount(t.getDeductionAmount());
        po.setDiscountRate(t.getDiscountRate());
        po.setTotalQuota(t.getTotalQuota());
        po.setPerUserLimit(t.getPerUserLimit());
        po.setIssuedCount(t.getIssuedCount());
        po.setValidStartTime(t.getValidStartTime());
        po.setValidEndTime(t.getValidEndTime());
        po.setActive(t.isActive());
        po.setCreateTime(t.getCreateTime());
        po.setUpdateTime(t.getUpdateTime());
        po.setVersion(t.getVersion());
        return po;
    }
}
