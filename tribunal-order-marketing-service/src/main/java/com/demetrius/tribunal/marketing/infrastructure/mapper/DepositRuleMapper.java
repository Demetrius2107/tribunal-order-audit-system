package com.demetrius.tribunal.marketing.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.marketing.infrastructure.model.DepositRulePo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 押金规则 Mapper。
 */
@Mapper
public interface DepositRuleMapper extends BaseMapper<DepositRulePo> {
}
