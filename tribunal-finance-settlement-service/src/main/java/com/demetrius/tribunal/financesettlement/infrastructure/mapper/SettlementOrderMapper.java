package com.demetrius.tribunal.financesettlement.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.financesettlement.infrastructure.model.SettlementOrderPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 结算单 Mapper。
 */
@Mapper
public interface SettlementOrderMapper extends BaseMapper<SettlementOrderPo> {
}
