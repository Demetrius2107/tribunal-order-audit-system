package com.demetrius.tribunal.marketing.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.marketing.infrastructure.model.UserCouponPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户券 Mapper。
 */
@Mapper
public interface UserCouponMapper extends BaseMapper<UserCouponPo> {
}
