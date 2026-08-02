package com.demetrius.tribunal.order.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.order.infrastructure.model.OrderStatusRecordPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单状态流水 Mapper。
 */
@Mapper
public interface OrderStatusRecordMapper extends BaseMapper<OrderStatusRecordPo> {
}
