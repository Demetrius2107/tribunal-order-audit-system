package com.demetrius.tribunal.financesettlement.infrastructure.repository;

import com.demetrius.tribunal.financesettlement.domain.model.AccountTransaction;
import com.demetrius.tribunal.financesettlement.domain.repository.AccountTransactionRepository;
import com.demetrius.tribunal.financesettlement.infrastructure.mapper.AccountTransactionMapper;
import com.demetrius.tribunal.financesettlement.infrastructure.model.AccountTransactionPo;
import org.springframework.stereotype.Repository;

/**
 * 账户流水仓储实现（infrastructure 层）。
 */
@Repository
public class AccountTransactionRepositoryImpl implements AccountTransactionRepository {

    private final AccountTransactionMapper accountTransactionMapper;

    public AccountTransactionRepositoryImpl(AccountTransactionMapper accountTransactionMapper) {
        this.accountTransactionMapper = accountTransactionMapper;
    }

    @Override
    public void save(AccountTransaction transaction) {
        AccountTransactionPo po = new AccountTransactionPo();
        po.setId(transaction.getId());
        po.setTransactionId(transaction.getTransactionId());
        po.setAccountId(transaction.getAccountId());
        po.setTransactionType(transaction.getTransactionType());
        po.setAmount(transaction.getAmount());
        po.setRelatedSettlementId(transaction.getRelatedSettlementId());
        po.setRelatedOrderId(transaction.getRelatedOrderId());
        po.setBalanceAfter(transaction.getBalanceAfter());
        po.setDescription(transaction.getDescription());
        accountTransactionMapper.insert(po);
    }
}
