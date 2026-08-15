package com.demetrius.tribunal.order.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.order.domain.model.CarPoolGroup;
import com.demetrius.tribunal.order.domain.model.CarPoolGroupStatus;
import com.demetrius.tribunal.order.domain.repository.CarPoolGroupRepository;
import com.demetrius.tribunal.order.infrastructure.mapper.CarPoolGroupMapper;
import com.demetrius.tribunal.order.infrastructure.mapper.CarPoolGroupMemberMapper;
import com.demetrius.tribunal.order.infrastructure.model.CarPoolGroupMemberPo;
import com.demetrius.tribunal.order.infrastructure.model.CarPoolGroupPo;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 拼车组仓储实现（MyBatis-Plus）。
 */
@Repository
public class CarPoolGroupRepositoryImpl implements CarPoolGroupRepository {

    private final CarPoolGroupMapper carPoolGroupMapper;

    private final CarPoolGroupMemberMapper carPoolGroupMemberMapper;

    public CarPoolGroupRepositoryImpl(CarPoolGroupMapper carPoolGroupMapper,
                                      CarPoolGroupMemberMapper carPoolGroupMemberMapper) {
        this.carPoolGroupMapper = carPoolGroupMapper;
        this.carPoolGroupMemberMapper = carPoolGroupMemberMapper;
    }

    @Override
    public void save(CarPoolGroup group) {
        CarPoolGroupPo po = toPo(group);
        CarPoolGroupPo exist = carPoolGroupMapper.selectById(group.getId());
        if (exist == null) {
            carPoolGroupMapper.insert(po);
        } else {
            carPoolGroupMapper.updateById(po);
        }
        // 成员同步：仅插入新增成员（保持 join_time 不变）
        List<String> existing = findMemberOrderNos(group.getId());
        for (String orderNo : group.getMemberOrderNos()) {
            if (!existing.contains(orderNo)) {
                CarPoolGroupMemberPo member = new CarPoolGroupMemberPo();
                member.setGroupId(group.getId());
                member.setOrderNo(orderNo);
                member.setJoinTime(LocalDateTime.now());
                carPoolGroupMemberMapper.insert(member);
            }
        }
    }

    @Override
    public Optional<CarPoolGroup> findById(String id) {
        CarPoolGroupPo po = carPoolGroupMapper.selectById(id);
        return po == null ? Optional.empty() : Optional.of(toDomain(po, findMemberOrderNos(id)));
    }

    @Override
    public Optional<CarPoolGroup> findByGroupNo(String groupNo) {
        CarPoolGroupPo po = carPoolGroupMapper.selectOne(
                new LambdaQueryWrapper<CarPoolGroupPo>().eq(CarPoolGroupPo::getGroupNo, groupNo));
        return po == null ? Optional.empty() : Optional.of(toDomain(po, findMemberOrderNos(po.getId())));
    }

    private List<String> findMemberOrderNos(String groupId) {
        return carPoolGroupMemberMapper.selectList(
                        new LambdaQueryWrapper<CarPoolGroupMemberPo>()
                                .eq(CarPoolGroupMemberPo::getGroupId, groupId)
                                .orderByAsc(CarPoolGroupMemberPo::getJoinTime))
                .stream()
                .map(CarPoolGroupMemberPo::getOrderNo)
                .toList();
    }

    private CarPoolGroup toDomain(CarPoolGroupPo po, List<String> memberOrderNos) {
        return CarPoolGroup.restore(
                po.getId(),
                po.getGroupNo(),
                CarPoolGroupStatus.valueOf(po.getStatus()),
                memberOrderNos,
                po.getCreateTime(),
                po.getUpdateTime());
    }

    private CarPoolGroupPo toPo(CarPoolGroup group) {
        CarPoolGroupPo po = new CarPoolGroupPo();
        po.setId(group.getId());
        po.setGroupNo(group.getGroupNo());
        po.setStatus(group.getStatus().name());
        po.setMemberCount(group.getMemberCount());
        po.setCreateTime(group.getCreateTime());
        po.setUpdateTime(group.getUpdateTime());
        return po;
    }
}
