package com.demetrius.tribunal.order.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.order.infrastructure.model.ReconcileRecordPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对账差异记录 Mapper（F-801/F-802：对账差异落库）。
 */
@Mapper
public interface ReconcileRecordMapper extends BaseMapper<ReconcileRecordPo> {
}
