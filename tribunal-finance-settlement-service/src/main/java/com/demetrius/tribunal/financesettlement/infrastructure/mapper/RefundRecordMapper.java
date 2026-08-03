package com.demetrius.tribunal.financesettlement.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.financesettlement.infrastructure.model.RefundRecordPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 退款记录 Mapper。
 */
@Mapper
public interface RefundRecordMapper extends BaseMapper<RefundRecordPo> {
}
