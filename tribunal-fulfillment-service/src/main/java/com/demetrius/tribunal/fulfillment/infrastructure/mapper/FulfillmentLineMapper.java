package com.demetrius.tribunal.fulfillment.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.fulfillment.infrastructure.model.FulfillmentLinePo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 履约单明细 Mapper。
 */
@Mapper
public interface FulfillmentLineMapper extends BaseMapper<FulfillmentLinePo> {

    @Select("SELECT * FROM t_fulfillment_line WHERE fulfillment_id = #{fulfillmentId} AND deleted = 0")
    List<FulfillmentLinePo> findByFulfillmentId(@Param("fulfillmentId") String fulfillmentId);
}
