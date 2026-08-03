package com.demetrius.tribunal.financesettlement.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.financesettlement.domain.model.PaymentIdempotent;
import com.demetrius.tribunal.financesettlement.domain.repository.PaymentIdempotentRepository;
import com.demetrius.tribunal.financesettlement.infrastructure.mapper.PaymentIdempotentMapper;
import com.demetrius.tribunal.financesettlement.infrastructure.model.PaymentIdempotentPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 扣款幂等记录仓储实现（infrastructure 层）。
 */
@Repository
public class PaymentIdempotentRepositoryImpl implements PaymentIdempotentRepository {

    private final PaymentIdempotentMapper paymentIdempotentMapper;

    public PaymentIdempotentRepositoryImpl(PaymentIdempotentMapper paymentIdempotentMapper) {
        this.paymentIdempotentMapper = paymentIdempotentMapper;
    }

    @Override
    public Optional<PaymentIdempotent> findByIdempotencyKey(String idempotencyKey) {
        PaymentIdempotentPo po = paymentIdempotentMapper.selectOne(
                new LambdaQueryWrapper<PaymentIdempotentPo>()
                        .eq(PaymentIdempotentPo::getIdempotencyKey, idempotencyKey));
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public void save(PaymentIdempotent record) {
        PaymentIdempotentPo po = new PaymentIdempotentPo();
        po.setIdempotencyKey(record.getIdempotencyKey());
        po.setSettlementId(record.getSettlementId());
        po.setStatus(record.getStatus());
        po.setChannelResponse(record.getChannelResponse());
        po.setExpireAt(record.getExpireAt());
        paymentIdempotentMapper.insert(po);
    }

    private PaymentIdempotent toDomain(PaymentIdempotentPo po) {
        return new PaymentIdempotent(
                po.getIdempotencyKey(), po.getSettlementId(), po.getStatus(),
                po.getChannelResponse(), po.getExpireAt());
    }
}
