package com.demetrius.tribunal.marketing.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.marketing.domain.model.CouponType;
import com.demetrius.tribunal.marketing.domain.model.UserCoupon;
import com.demetrius.tribunal.marketing.domain.model.UserCouponStatus;
import com.demetrius.tribunal.marketing.domain.repository.UserCouponRepository;
import com.demetrius.tribunal.marketing.infrastructure.mapper.UserCouponMapper;
import com.demetrius.tribunal.marketing.infrastructure.model.UserCouponPo;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户券仓储实现。
 */
@Repository
public class UserCouponRepositoryImpl implements UserCouponRepository {

    private final UserCouponMapper mapper;

    public UserCouponRepositoryImpl(UserCouponMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(UserCoupon userCoupon) {
        UserCouponPo po = toPo(userCoupon);
        UserCouponPo existing = mapper.selectById(po.getId());
        if (existing == null) {
            mapper.insert(po);
        } else {
            // 乐观锁：version 不匹配时影响行数为 0（并发重复核销/重复操作冲突）
            if (mapper.updateById(po) == 0) {
                throw new BizException("409202", "券已被并发操作，请重试");
            }
        }
    }

    @Override
    public Optional<UserCoupon> findById(String id) {
        UserCouponPo po = mapper.selectById(id);
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public Optional<UserCoupon> findByCouponCode(String couponCode) {
        UserCouponPo po = mapper.selectOne(
                new LambdaQueryWrapper<UserCouponPo>()
                        .eq(UserCouponPo::getCouponCode, couponCode));
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public List<UserCoupon> findByCustomerId(String customerId) {
        return mapper.selectList(
                        new LambdaQueryWrapper<UserCouponPo>()
                                .eq(UserCouponPo::getCustomerId, customerId)
                                .orderByDesc(UserCouponPo::getReceiveTime))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<UserCoupon> findByCustomerIdAndStatus(String customerId, UserCouponStatus status) {
        return mapper.selectList(
                        new LambdaQueryWrapper<UserCouponPo>()
                                .eq(UserCouponPo::getCustomerId, customerId)
                                .eq(UserCouponPo::getStatus, status.name())
                                .orderByDesc(UserCouponPo::getReceiveTime))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public int countByCustomerIdAndTemplateId(String customerId, String templateId) {
        return Math.toIntExact(mapper.selectCount(
                new LambdaQueryWrapper<UserCouponPo>()
                        .eq(UserCouponPo::getCustomerId, customerId)
                        .eq(UserCouponPo::getTemplateId, templateId)));
    }

    @Override
    public List<UserCoupon> findExpiredNotProcessed() {
        LocalDateTime now = LocalDateTime.now();
        return mapper.selectList(
                        new LambdaQueryWrapper<UserCouponPo>()
                                .lt(UserCouponPo::getValidEndTime, now)
                                .in(UserCouponPo::getStatus,
                                        UserCouponStatus.AVAILABLE.name(),
                                        UserCouponStatus.LOCKED.name()))
                .stream().map(this::toDomain).toList();
    }

    // ---------- 转换 ----------

    private UserCoupon toDomain(UserCouponPo po) {
        return UserCoupon.restore(
                po.getId(), po.getCouponCode(), po.getTemplateId(), po.getTemplateNo(),
                po.getCustomerId(),
                CouponType.valueOf(po.getType()),
                po.getThreshold(), po.getDeductionAmount(), po.getDiscountRate(),
                UserCouponStatus.valueOf(po.getStatus()),
                po.getOrderId(),
                po.getReceiveTime(), po.getValidStartTime(), po.getValidEndTime(),
                po.getUsedTime(),
                po.getVersion() == null ? 0 : po.getVersion());
    }

    private UserCouponPo toPo(UserCoupon c) {
        UserCouponPo po = new UserCouponPo();
        po.setId(c.getId());
        po.setCouponCode(c.getCouponCode());
        po.setTemplateId(c.getTemplateId());
        po.setTemplateNo(c.getTemplateNo());
        po.setCustomerId(c.getCustomerId());
        po.setType(c.getType().name());
        po.setThreshold(c.getThreshold());
        po.setDeductionAmount(c.getDeductionAmount());
        po.setDiscountRate(c.getDiscountRate());
        po.setStatus(c.getStatus().name());
        po.setOrderId(c.getOrderId());
        po.setReceiveTime(c.getReceiveTime());
        po.setValidStartTime(c.getValidStartTime());
        po.setValidEndTime(c.getValidEndTime());
        po.setUsedTime(c.getUsedTime());
        po.setVersion(c.getVersion());
        return po;
    }
}
