package com.demetrius.tribunal.financesettlement.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.financesettlement.domain.model.SplitRecord;
import com.demetrius.tribunal.financesettlement.domain.repository.SplitRecordRepository;
import com.demetrius.tribunal.financesettlement.infrastructure.mapper.SplitRecordMapper;
import com.demetrius.tribunal.financesettlement.infrastructure.model.SplitRecordPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 分账记录仓储实现（infrastructure 层）。
 */
@Repository
public class SplitRecordRepositoryImpl implements SplitRecordRepository {

    private final SplitRecordMapper splitRecordMapper;

    public SplitRecordRepositoryImpl(SplitRecordMapper splitRecordMapper) {
        this.splitRecordMapper = splitRecordMapper;
    }

    @Override
    public List<SplitRecord> findBySettlementId(String settlementId) {
        return splitRecordMapper.selectList(
                        new LambdaQueryWrapper<SplitRecordPo>()
                                .eq(SplitRecordPo::getSettlementId, settlementId))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void save(SplitRecord record) {
        SplitRecordPo po = new SplitRecordPo();
        po.setId(record.getId());
        po.setSettlementId(record.getSettlementId());
        po.setRecipientId(record.getRecipientId());
        po.setRecipientType(record.getRecipientType());
        po.setSplitAmount(record.getSplitAmount());
        po.setSplitRate(record.getSplitRate());
        po.setStatus(record.getStatus());
        po.setChannelTransactionId(record.getChannelTransactionId());
        splitRecordMapper.insert(po);
    }

    private SplitRecord toDomain(SplitRecordPo po) {
        return new SplitRecord(
                po.getId(), po.getSettlementId(), po.getRecipientId(), po.getRecipientType(),
                po.getSplitAmount(), po.getSplitRate(), po.getStatus(), po.getChannelTransactionId());
    }
}
