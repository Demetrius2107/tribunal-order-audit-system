package com.demetrius.tribunal.inventory.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.inventory.infrastructure.model.InventoryItemPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存物料 Mapper。
 */
@Mapper
public interface InventoryItemMapper extends BaseMapper<InventoryItemPo> {
}
