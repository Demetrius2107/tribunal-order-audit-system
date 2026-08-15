package com.demetrius.tribunal.order.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.order.infrastructure.model.CarPoolGroupMemberPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 拼车组成员 Mapper（F-310）。
 */
@Mapper
public interface CarPoolGroupMemberMapper extends BaseMapper<CarPoolGroupMemberPo> {
}
