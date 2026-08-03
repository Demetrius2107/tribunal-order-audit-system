package com.demetrius.tribunal.financesettlement.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.financesettlement.infrastructure.model.AccountBalancePo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分账方账户余额 Mapper。
 */
@Mapper
public interface AccountBalanceMapper extends BaseMapper<AccountBalancePo> {
}
