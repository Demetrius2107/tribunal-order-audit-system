package com.demetrius.tribunal.order.domain.repository;

import com.demetrius.tribunal.order.domain.model.CarPoolGroup;

import java.util.Optional;

/**
 * 拼车组仓储接口（F-310：domain 定义，infrastructure 实现）。
 */
public interface CarPoolGroupRepository {

    /** 保存拼车组（新增 + 修改，含成员同步）。 */
    void save(CarPoolGroup group);

    /** 按 ID 查询拼车组（含成员）。 */
    Optional<CarPoolGroup> findById(String id);

    /** 按拼车组编号查询（业务唯一键）。 */
    Optional<CarPoolGroup> findByGroupNo(String groupNo);
}
