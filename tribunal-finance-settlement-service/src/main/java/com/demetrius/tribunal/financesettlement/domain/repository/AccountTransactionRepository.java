package com.demetrius.tribunal.financesettlement.domain.repository;

import com.demetrius.tribunal.financesettlement.domain.model.AccountTransaction;

/**
 * 账户流水仓储接口。
 */
public interface AccountTransactionRepository {

    void save(AccountTransaction transaction);
}
