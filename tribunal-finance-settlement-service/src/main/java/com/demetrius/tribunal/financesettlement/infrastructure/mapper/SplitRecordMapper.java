package com.demetrius.tribunal.financesettlement.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.financesettlement.infrastructure.model.SplitRecordPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分账记录 Mapper。
 */
@Mapper
public interface SplitRecordMapper extends BaseMapper<SplitRecordPo> {
}
