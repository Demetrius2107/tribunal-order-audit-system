package com.demetrius.tribunal.order.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.order.domain.model.PreOrderRecord;
import com.demetrius.tribunal.order.domain.repository.PreOrderRecordRepository;
import com.demetrius.tribunal.order.infrastructure.mapper.PreOrderRecordMapper;
import com.demetrius.tribunal.order.infrastructure.model.PreOrderRecordPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 预购订单记录仓储实现（MyBatis-Plus）。
 */
@Repository
public class PreOrderRecordRepositoryImpl implements PreOrderRecordRepository {

    private final PreOrderRecordMapper preOrderRecordMapper;

    public PreOrderRecordRepositoryImpl(PreOrderRecordMapper preOrderRecordMapper) {
        this.preOrderRecordMapper = preOrderRecordMapper;
    }

    @Override
    public void save(PreOrderRecord record) {
        PreOrderRecordPo po = toPo(record);
        PreOrderRecordPo exist = findPo(record.getActivityNo(), record.getOrderNo());
        if (exist == null) {
            preOrderRecordMapper.insert(po);
        } else {
            po.setId(exist.getId());
            preOrderRecordMapper.updateById(po);
        }
    }

    @Override
    public Optional<PreOrderRecord> findByActivityNoAndOrderNo(String activityNo, String orderNo) {
        return Optional.ofNullable(findPo(activityNo, orderNo)).map(this::toDomain);
    }

    @Override
    public void deleteByOrderNo(String orderNo) {
        preOrderRecordMapper.delete(
                new LambdaQueryWrapper<PreOrderRecordPo>().eq(PreOrderRecordPo::getOrderNo, orderNo));
    }

    private PreOrderRecordPo findPo(String activityNo, String orderNo) {
        return preOrderRecordMapper.selectOne(
                new LambdaQueryWrapper<PreOrderRecordPo>()
                        .eq(PreOrderRecordPo::getActivityNo, activityNo)
                        .eq(PreOrderRecordPo::getOrderNo, orderNo));
    }

    private PreOrderRecord toDomain(PreOrderRecordPo po) {
        return new PreOrderRecord(
                po.getId(),
                po.getActivityNo(),
                po.getOrderNo(),
                po.getTotalAmount(),
                po.getDepositAmount(),
                po.getSupplementAmount(),
                po.getCreateTime());
    }

    private PreOrderRecordPo toPo(PreOrderRecord record) {
        PreOrderRecordPo po = new PreOrderRecordPo();
        po.setId(record.getId());
        po.setActivityNo(record.getActivityNo());
        po.setOrderNo(record.getOrderNo());
        po.setTotalAmount(record.getTotalAmount());
        po.setDepositAmount(record.getDepositAmount());
        po.setSupplementAmount(record.getSupplementAmount());
        po.setCreateTime(record.getCreateTime());
        return po;
    }
}
