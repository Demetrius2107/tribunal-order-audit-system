package com.demetrius.tribunal.customer.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.customer.infrastructure.model.CustomerPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户 Mapper。
 */
@Mapper
public interface CustomerMapper extends BaseMapper<CustomerPo> {
}
