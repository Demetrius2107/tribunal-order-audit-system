package com.demetrius.tribunal.inventorypush.domain.repository;

import com.demetrius.tribunal.inventorypush.domain.model.IdempotentRecord;

import java.util.Optional;

/**
 * 幂等记录仓储接口。
 */
public interface IdempotentRecordRepository {

    Optional<IdempotentRecord> findByIdempotencyKey(String idempotencyKey);

    void save(IdempotentRecord record);
}
