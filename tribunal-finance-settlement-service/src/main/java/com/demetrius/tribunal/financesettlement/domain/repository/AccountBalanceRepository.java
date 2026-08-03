package com.demetrius.tribunal.financesettlement.domain.repository;

import com.demetrius.tribunal.financesettlement.domain.model.AccountBalance;

import java.util.Optional;

/**
 * 分账方账户仓储接口。
 */
public interface AccountBalanceRepository {

    Optional<AccountBalance> findByAccountId(String accountId);

    void save(AccountBalance account);

    /** 乐观锁更新，失败返回 false 由应用层重试 */
    boolean updateWithVersion(AccountBalance account);
}
