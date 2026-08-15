package com.demetrius.tribunal.order.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.order.domain.model.PreOrderActivity;
import com.demetrius.tribunal.order.domain.model.PreOrderActivityStatus;
import com.demetrius.tribunal.order.domain.repository.PreOrderActivityRepository;
import com.demetrius.tribunal.order.infrastructure.mapper.PreOrderActivityMapper;
import com.demetrius.tribunal.order.infrastructure.model.PreOrderActivityPo;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 预购活动仓储实现（MyBatis-Plus）。
 */
@Repository
public class PreOrderActivityRepositoryImpl implements PreOrderActivityRepository {

    private final PreOrderActivityMapper preOrderActivityMapper;

    public PreOrderActivityRepositoryImpl(PreOrderActivityMapper preOrderActivityMapper) {
        this.preOrderActivityMapper = preOrderActivityMapper;
    }

    @Override
    public void save(PreOrderActivity activity) {
        PreOrderActivityPo po = toPo(activity);
        PreOrderActivityPo exist = preOrderActivityMapper.selectById(activity.getId());
        if (exist == null) {
            preOrderActivityMapper.insert(po);
        } else {
            preOrderActivityMapper.updateById(po);
        }
    }

    @Override
    public Optional<PreOrderActivity> findById(String id) {
        PreOrderActivityPo po = preOrderActivityMapper.selectById(id);
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public Optional<PreOrderActivity> findByActivityNo(String activityNo) {
        PreOrderActivityPo po = preOrderActivityMapper.selectOne(
                new LambdaQueryWrapper<PreOrderActivityPo>().eq(PreOrderActivityPo::getActivityNo, activityNo));
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    private PreOrderActivity toDomain(PreOrderActivityPo po) {
        return PreOrderActivity.restore(
                po.getId(),
                po.getActivityNo(),
                po.getName(),
                splitSkuCodes(po.getSkuCodes()),
                po.getDepositRate(),
                po.getDiscountRate(),
                po.getStartTime(),
                po.getEndTime(),
                PreOrderActivityStatus.valueOf(po.getStatus()),
                po.getCreateTime(),
                po.getUpdateTime());
    }

    private PreOrderActivityPo toPo(PreOrderActivity activity) {
        PreOrderActivityPo po = new PreOrderActivityPo();
        po.setId(activity.getId());
        po.setActivityNo(activity.getActivityNo());
        po.setName(activity.getName());
        po.setSkuCodes(String.join(",", activity.getSkuCodes()));
        po.setDepositRate(activity.getDepositRate());
        po.setDiscountRate(activity.getDiscountRate());
        po.setStartTime(activity.getStartTime());
        po.setEndTime(activity.getEndTime());
        po.setStatus(activity.getStatus().name());
        po.setCreateTime(activity.getCreateTime());
        po.setUpdateTime(activity.getUpdateTime());
        return po;
    }

    private List<String> splitSkuCodes(String skuCodes) {
        if (skuCodes == null || skuCodes.isBlank()) {
            return List.of();
        }
        return Arrays.stream(skuCodes.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
