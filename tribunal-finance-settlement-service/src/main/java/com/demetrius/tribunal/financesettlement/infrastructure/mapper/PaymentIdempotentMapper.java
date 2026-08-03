package com.demetrius.tribunal.financesettlement.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.financesettlement.infrastructure.model.PaymentIdempotentPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 扣款幂等记录 Mapper。
 */
@Mapper
public interface PaymentIdempotentMapper extends BaseMapper<PaymentIdempotentPo> {
}
