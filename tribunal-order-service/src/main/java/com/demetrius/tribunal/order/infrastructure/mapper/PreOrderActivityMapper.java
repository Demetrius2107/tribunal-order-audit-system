package com.demetrius.tribunal.order.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.order.infrastructure.model.PreOrderActivityPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预购活动 Mapper（F-312）。
 */
@Mapper
public interface PreOrderActivityMapper extends BaseMapper<PreOrderActivityPo> {
}
