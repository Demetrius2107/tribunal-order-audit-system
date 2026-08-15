package com.demetrius.tribunal.inventory.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.inventory.infrastructure.model.InventoryFlowPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存变动流水 Mapper。
 */
@Mapper
public interface InventoryFlowMapper extends BaseMapper<InventoryFlowPo> {
}
