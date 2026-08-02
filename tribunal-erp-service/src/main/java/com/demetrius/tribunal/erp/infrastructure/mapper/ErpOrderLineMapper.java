package com.demetrius.tribunal.erp.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.erp.infrastructure.model.ErpOrderLinePo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * ERP 履约订单明细 Mapper。
 */
@Mapper
public interface ErpOrderLineMapper extends BaseMapper<ErpOrderLinePo> {

    @Select("SELECT * FROM t_erp_order_line WHERE erp_order_id = #{erpOrderId} AND deleted = 0")
    List<ErpOrderLinePo> findByErpOrderId(@Param("erpOrderId") String erpOrderId);
}
