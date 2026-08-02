package com.demetrius.tribunal.billing.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.billing.infrastructure.model.BillPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 金融账单订单 Mapper。
 */
@Mapper
public interface BillMapper extends BaseMapper<BillPo> {
}
