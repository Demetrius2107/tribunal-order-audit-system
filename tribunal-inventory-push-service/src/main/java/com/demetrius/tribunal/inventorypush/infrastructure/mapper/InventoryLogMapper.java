package com.demetrius.tribunal.inventorypush.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.inventorypush.infrastructure.model.InventoryLogPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存流水 Mapper。
 */
@Mapper
public interface InventoryLogMapper extends BaseMapper<InventoryLogPo> {
}
