package com.demetrius.tribunal.billing.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.billing.infrastructure.model.ReconcileRecordPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对账差异记录 Mapper（F-607：财务对账差异落库）。
 */
@Mapper
public interface ReconcileRecordMapper extends BaseMapper<ReconcileRecordPo> {
}
