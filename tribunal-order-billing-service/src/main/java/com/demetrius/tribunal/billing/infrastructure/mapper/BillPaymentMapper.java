package com.demetrius.tribunal.billing.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.billing.infrastructure.model.BillPaymentPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收款流水 Mapper（F-606：审计 + 对账）。
 */
@Mapper
public interface BillPaymentMapper extends BaseMapper<BillPaymentPo> {
}
