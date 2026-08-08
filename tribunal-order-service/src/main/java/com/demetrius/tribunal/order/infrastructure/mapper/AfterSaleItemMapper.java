package com.demetrius.tribunal.order.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.order.infrastructure.model.AfterSaleItemPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 售后明细 Mapper。
 */
@Mapper
public interface AfterSaleItemMapper extends BaseMapper<AfterSaleItemPo> {

    @Select("SELECT * FROM t_after_sale_item WHERE after_sale_id = #{afterSaleId} AND deleted = 0")
    List<AfterSaleItemPo> findByAfterSaleId(@Param("afterSaleId") String afterSaleId);
}
