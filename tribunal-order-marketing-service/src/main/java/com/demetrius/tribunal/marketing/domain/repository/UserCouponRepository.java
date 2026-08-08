package com.demetrius.tribunal.marketing.domain.repository;

import com.demetrius.tribunal.marketing.domain.model.UserCoupon;
import com.demetrius.tribunal.marketing.domain.model.UserCouponStatus;

import java.util.List;
import java.util.Optional;

/**
 * 用户券仓储接口。
 */
public interface UserCouponRepository {

    void save(UserCoupon userCoupon);

    Optional<UserCoupon> findById(String id);

    Optional<UserCoupon> findByCouponCode(String couponCode);

    /** 查询用户的所有券 */
    List<UserCoupon> findByCustomerId(String customerId);

    /** 查询用户某状态的券 */
    List<UserCoupon> findByCustomerIdAndStatus(String customerId, UserCouponStatus status);

    /** 统计用户已领取某模板的券数量（用于 perUserLimit 校验） */
    int countByCustomerIdAndTemplateId(String customerId, String templateId);

    /** 查询所有已过期但仍为 AVAILABLE/LOCKED 状态的券（供定时回收） */
    List<UserCoupon> findExpiredNotProcessed();
}
