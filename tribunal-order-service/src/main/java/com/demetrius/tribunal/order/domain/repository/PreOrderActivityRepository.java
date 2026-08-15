package com.demetrius.tribunal.order.domain.repository;

import com.demetrius.tribunal.order.domain.model.PreOrderActivity;

import java.util.Optional;

/**
 * 预购活动仓储接口（F-312：domain 定义，infrastructure 实现）。
 */
public interface PreOrderActivityRepository {

    /** 保存预购活动（新增 + 修改）。 */
    void save(PreOrderActivity activity);

    /** 按 ID 查询预购活动。 */
    Optional<PreOrderActivity> findById(String id);

    /** 按活动编号查询（业务唯一键）。 */
    Optional<PreOrderActivity> findByActivityNo(String activityNo);
}
