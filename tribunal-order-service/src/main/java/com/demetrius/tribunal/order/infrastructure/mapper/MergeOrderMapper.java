package com.demetrius.tribunal.order.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.order.infrastructure.model.MergeOrderPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 合单 Mapper。
 */
@Mapper
public interface MergeOrderMapper extends BaseMapper<MergeOrderPo> {
}
