package com.demetrius.tribunal.order.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.order.infrastructure.model.OrderSkuPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 订单明细 Mapper。
 */
@Mapper
public interface OrderSkuMapper extends BaseMapper<OrderSkuPo> {

    /** 按订单 ID 查明细（参照通用做法 */
    @Select("SELECT * FROM t_order_sku WHERE order_id = #{orderId} AND deleted = 0")
    List<OrderSkuPo> findByOrderId(@Param("orderId") String orderId);
}
