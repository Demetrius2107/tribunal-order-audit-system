package com.demetrius.tribunal.erp.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.erp.infrastructure.model.ErpOrderPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 履约订单 Mapper。
 */
@Mapper
public interface ErpOrderMapper extends BaseMapper<ErpOrderPo> {
}
