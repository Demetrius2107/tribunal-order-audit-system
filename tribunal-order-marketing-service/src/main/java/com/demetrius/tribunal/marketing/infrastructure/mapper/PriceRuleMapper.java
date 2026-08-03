package com.demetrius.tribunal.marketing.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.marketing.infrastructure.model.PriceRulePo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 价格规则 Mapper。
 */
@Mapper
public interface PriceRuleMapper extends BaseMapper<PriceRulePo> {
}
