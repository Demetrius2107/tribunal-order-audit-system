package com.demetrius.tribunal.fulfillment.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.fulfillment.infrastructure.model.FulfillmentOrderPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 履约单 Mapper。
 */
@Mapper
public interface FulfillmentOrderMapper extends BaseMapper<FulfillmentOrderPo> {
}
