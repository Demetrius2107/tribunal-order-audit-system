package com.demetrius.tribunal.marketing.domain.repository;

import com.demetrius.tribunal.marketing.domain.model.CouponTemplate;

import java.util.List;
import java.util.Optional;

/**
 * 券模板仓储接口。
 */
public interface CouponTemplateRepository {

    void save(CouponTemplate template);

    Optional<CouponTemplate> findById(String id);

    Optional<CouponTemplate> findByTemplateNo(String templateNo);

    /** 查询所有启用的券模板（供用户浏览可领券列表） */
    List<CouponTemplate> findAllActive();
}
