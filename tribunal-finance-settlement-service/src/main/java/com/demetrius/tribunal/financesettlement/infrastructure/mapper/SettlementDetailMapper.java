package com.demetrius.tribunal.financesettlement.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.financesettlement.infrastructure.model.SettlementDetailPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 结算明细 Mapper。
 */
@Mapper
public interface SettlementDetailMapper extends BaseMapper<SettlementDetailPo> {
}
