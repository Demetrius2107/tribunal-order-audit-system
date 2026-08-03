package com.demetrius.tribunal.financesettlement.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.financesettlement.domain.model.AccountBalance;
import com.demetrius.tribunal.financesettlement.domain.repository.AccountBalanceRepository;
import com.demetrius.tribunal.financesettlement.infrastructure.mapper.AccountBalanceMapper;
import com.demetrius.tribunal.financesettlement.infrastructure.model.AccountBalancePo;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 分账方账户仓储实现（infrastructure 层）。
 */
@Repository
public class AccountBalanceRepositoryImpl implements AccountBalanceRepository {

    private final AccountBalanceMapper accountBalanceMapper;

    public AccountBalanceRepositoryImpl(AccountBalanceMapper accountBalanceMapper) {
        this.accountBalanceMapper = accountBalanceMapper;
    }

    @Override
    public Optional<AccountBalance> findByAccountId(String accountId) {
        AccountBalancePo po = accountBalanceMapper.selectOne(
                new LambdaQueryWrapper<AccountBalancePo>()
                        .eq(AccountBalancePo::getAccountId, accountId));
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public void save(AccountBalance account) {
        AccountBalancePo po = toPo(account);
        if (accountBalanceMapper.selectById(po.getId()) == null) {
            accountBalanceMapper.insert(po);
        } else {
            accountBalanceMapper.updateById(po);
        }
    }

    @Override
    public boolean updateWithVersion(AccountBalance account) {
        // MyBatis-Plus 乐观锁：PO 带 @Version 后 updateById 失败返回 0
        return accountBalanceMapper.updateById(toPo(account)) > 0;
    }

    private AccountBalance toDomain(AccountBalancePo po) {
        return new AccountBalance(
                po.getId(), po.getAccountId(), po.getOwnerId(), po.getOwnerType(),
                po.getAvailableBalance() == null ? BigDecimal.ZERO : po.getAvailableBalance(),
                po.getFrozenBalance() == null ? BigDecimal.ZERO : po.getFrozenBalance(),
                po.getInTransitAmount() == null ? BigDecimal.ZERO : po.getInTransitAmount(),
                po.getCurrency(), po.getVersion() == null ? 0 : po.getVersion());
    }

    private AccountBalancePo toPo(AccountBalance account) {
        AccountBalancePo po = new AccountBalancePo();
        po.setId(account.getId());
        po.setAccountId(account.getAccountId());
        po.setOwnerId(account.getOwnerId());
        po.setOwnerType(account.getOwnerType());
        po.setAvailableBalance(account.getAvailableBalance());
        po.setFrozenBalance(account.getFrozenBalance());
        po.setInTransitAmount(account.getInTransitAmount());
        po.setCurrency(account.getCurrency());
        po.setVersion(account.getVersion());
        return po;
    }
}
