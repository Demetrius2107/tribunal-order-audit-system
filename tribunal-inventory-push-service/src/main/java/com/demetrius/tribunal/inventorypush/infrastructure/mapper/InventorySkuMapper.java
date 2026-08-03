package com.demetrius.tribunal.inventorypush.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.inventorypush.infrastructure.model.InventorySkuPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存主数据 Mapper。
 */
@Mapper
public interface InventorySkuMapper extends BaseMapper<InventorySkuPo> {
}
