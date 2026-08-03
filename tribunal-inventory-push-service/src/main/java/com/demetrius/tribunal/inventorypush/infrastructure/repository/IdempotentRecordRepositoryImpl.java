package com.demetrius.tribunal.inventorypush.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.inventorypush.domain.model.IdempotentRecord;
import com.demetrius.tribunal.inventorypush.domain.repository.IdempotentRecordRepository;
import com.demetrius.tribunal.inventorypush.infrastructure.mapper.IdempotentRecordMapper;
import com.demetrius.tribunal.inventorypush.infrastructure.model.IdempotentRecordPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 幂等记录仓储实现（infrastructure 层）。
 */
@Repository
public class IdempotentRecordRepositoryImpl implements IdempotentRecordRepository {

    private final IdempotentRecordMapper idempotentRecordMapper;

    public IdempotentRecordRepositoryImpl(IdempotentRecordMapper idempotentRecordMapper) {
        this.idempotentRecordMapper = idempotentRecordMapper;
    }

    @Override
    public Optional<IdempotentRecord> findByIdempotencyKey(String idempotencyKey) {
        IdempotentRecordPo po = idempotentRecordMapper.selectOne(
                new LambdaQueryWrapper<IdempotentRecordPo>()
                        .eq(IdempotentRecordPo::getIdempotencyKey, idempotencyKey));
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public void save(IdempotentRecord record) {
        IdempotentRecordPo po = new IdempotentRecordPo();
        po.setIdempotencyKey(record.getIdempotencyKey());
        po.setBatchId(record.getBatchId());
        po.setStatus(record.getStatus());
        po.setExpireAt(record.getExpireAt());
        idempotentRecordMapper.insert(po);
    }

    private IdempotentRecord toDomain(IdempotentRecordPo po) {
        return new IdempotentRecord(po.getIdempotencyKey(), po.getBatchId(), po.getStatus(), po.getExpireAt());
    }
}
