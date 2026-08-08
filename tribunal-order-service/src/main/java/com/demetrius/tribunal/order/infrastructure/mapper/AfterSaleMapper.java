package com.demetrius.tribunal.order.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.order.infrastructure.model.AfterSalePo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 售后单 Mapper。
 */
@Mapper
public interface AfterSaleMapper extends BaseMapper<AfterSalePo> {
}
