package com.demetrius.tribunal.order.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.order.infrastructure.model.ReturnablePackagingPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 空包装回收明细 Mapper。
 */
@Mapper
public interface ReturnablePackagingMapper extends BaseMapper<ReturnablePackagingPo> {

    /** 按订单 ID 查回收明细 */
    @Select("SELECT * FROM t_order_returnable WHERE order_id = #{orderId} AND deleted = 0")
    List<ReturnablePackagingPo> findByOrderId(@Param("orderId") String orderId);
}
