package com.demetrius.tribunal.inventorypush.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.inventorypush.infrastructure.model.InventoryBatchPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 批次库存 Mapper。
 */
@Mapper
public interface InventoryBatchMapper extends BaseMapper<InventoryBatchPo> {
}
