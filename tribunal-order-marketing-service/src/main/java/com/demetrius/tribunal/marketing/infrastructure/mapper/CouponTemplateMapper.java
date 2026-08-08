package com.demetrius.tribunal.marketing.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.marketing.infrastructure.model.CouponTemplatePo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券模板 Mapper。
 */
@Mapper
public interface CouponTemplateMapper extends BaseMapper<CouponTemplatePo> {
}
