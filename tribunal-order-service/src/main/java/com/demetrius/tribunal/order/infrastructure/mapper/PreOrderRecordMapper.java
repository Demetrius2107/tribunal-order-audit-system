package com.demetrius.tribunal.order.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.order.infrastructure.model.PreOrderRecordPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预购订单记录 Mapper（F-312）。
 */
@Mapper
public interface PreOrderRecordMapper extends BaseMapper<PreOrderRecordPo> {
}
