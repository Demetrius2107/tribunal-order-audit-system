package com.demetrius.tribunal.financesettlement.domain.repository;

import com.demetrius.tribunal.financesettlement.domain.model.PaymentIdempotent;

import java.util.Optional;

/**
 * 扣款幂等记录仓储接口。
 */
public interface PaymentIdempotentRepository {

    Optional<PaymentIdempotent> findByIdempotencyKey(String idempotencyKey);

    void save(PaymentIdempotent record);
}
