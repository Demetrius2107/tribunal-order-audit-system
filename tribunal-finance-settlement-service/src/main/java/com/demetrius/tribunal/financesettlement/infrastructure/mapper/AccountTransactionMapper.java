package com.demetrius.tribunal.financesettlement.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.financesettlement.infrastructure.model.AccountTransactionPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账户流水 Mapper。
 */
@Mapper
public interface AccountTransactionMapper extends BaseMapper<AccountTransactionPo> {
}
