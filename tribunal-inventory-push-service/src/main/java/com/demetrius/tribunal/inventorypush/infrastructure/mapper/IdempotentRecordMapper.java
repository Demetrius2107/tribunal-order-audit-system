package com.demetrius.tribunal.inventorypush.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.inventorypush.infrastructure.model.IdempotentRecordPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 幂等记录 Mapper。
 */
@Mapper
public interface IdempotentRecordMapper extends BaseMapper<IdempotentRecordPo> {
}
