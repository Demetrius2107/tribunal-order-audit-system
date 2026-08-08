package com.demetrius.tribunal.marketing.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.marketing.infrastructure.model.PromotionRulePo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 促销规则 Mapper。
 */
@Mapper
public interface PromotionRuleMapper extends BaseMapper<PromotionRulePo> {
}
