package com.demetrius.tribunal.order.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.order.infrastructure.model.MergeOrderItemPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 合单明细 Mapper。
 */
@Mapper
public interface MergeOrderItemMapper extends BaseMapper<MergeOrderItemPo> {

    @Select("SELECT * FROM t_merge_order_item WHERE merge_order_id = #{mergeOrderId} AND deleted = 0")
    List<MergeOrderItemPo> findByMergeOrderId(@Param("mergeOrderId") String mergeOrderId);
}
