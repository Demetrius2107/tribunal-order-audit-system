package com.demetrius.tribunal.marketing.application.service;

import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.marketing.application.dto.CouponTemplateResult;
import com.demetrius.tribunal.marketing.application.dto.UserCouponResult;
import com.demetrius.tribunal.marketing.domain.model.CouponTemplate;
import com.demetrius.tribunal.marketing.domain.model.CouponType;
import com.demetrius.tribunal.marketing.domain.model.UserCoupon;
import com.demetrius.tribunal.marketing.domain.model.UserCouponStatus;
import com.demetrius.tribunal.marketing.domain.repository.CouponTemplateRepository;
import com.demetrius.tribunal.marketing.domain.repository.UserCouponRepository;
import com.demetrius.tribunal.marketing.domain.service.CouponRedemptionEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 优惠券应用服务（用例编排层）。
 *
 * <p>编排全流程：创建模板 → 领券 → 锁定 → 核销/释放 → 过期回收。</p>
 *
 * <p>防刷规则：</p>
 * <ul>
 *   <li>领券时校验 perUserLimit（每人限领数量）</li>
 *   <li>领券时校验 totalQuota（总发放量，防超发）</li>
 *   <li>核销时校验券状态（AVAILABLE/LOCKED）和有效期</li>
 * </ul>
 */
@Service
public class CouponApplicationService {

    private static final Logger log = LoggerFactory.getLogger(CouponApplicationService.class);

    private final CouponTemplateRepository templateRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponRedemptionEngine redemptionEngine;

    public CouponApplicationService(CouponTemplateRepository templateRepository,
                                     UserCouponRepository userCouponRepository,
                                     CouponRedemptionEngine redemptionEngine) {
        this.templateRepository = templateRepository;
        this.userCouponRepository = userCouponRepository;
        this.redemptionEngine = redemptionEngine;
    }

    // ===== 券模板管理 =====

    /**
     * 创建券模板。
     */
    @Transactional
    public CouponTemplateResult createTemplate(String name, String typeStr,
                                                BigDecimal threshold, BigDecimal deductionAmount,
                                                BigDecimal discountRate,
                                                Integer totalQuota, int perUserLimit,
                                                LocalDateTime validStartTime, LocalDateTime validEndTime) {
        CouponType type = CouponType.valueOf(typeStr);
        String id = generateId();
        String templateNo = generateTemplateNo();
        CouponTemplate template = CouponTemplate.create(id, templateNo, name, type,
                threshold, deductionAmount, discountRate,
                totalQuota, perUserLimit,
                validStartTime, validEndTime);
        templateRepository.save(template);
        log.info("券模板已创建: templateNo={}, type={}", templateNo, type);
        return CouponTemplateResult.from(template);
    }

    /** 查询券模板详情 */
    @Transactional(readOnly = true)
    public CouponTemplateResult getTemplate(String templateId) {
        return CouponTemplateResult.from(loadTemplateOrThrow(templateId));
    }

    /** 查询所有可领的券模板 */
    @Transactional(readOnly = true)
    public List<CouponTemplateResult> listActiveTemplates() {
        return templateRepository.findAllActive().stream()
                .map(CouponTemplateResult::from)
                .toList();
    }

    /** 停用券模板 */
    @Transactional
    public CouponTemplateResult deactivateTemplate(String templateId) {
        CouponTemplate template = loadTemplateOrThrow(templateId);
        template.deactivate();
        templateRepository.save(template);
        log.info("券模板已停用: templateNo={}", template.getTemplateNo());
        return CouponTemplateResult.from(template);
    }

    // ===== 用户券操作 =====

    /**
     * 领券（含防刷校验）。
     *
     * @param templateId 券模板 ID
     * @param customerId 领用人
     * @return 用户券
     */
    @Transactional
    public UserCouponResult receive(String templateId, String customerId) {
        CouponTemplate template = loadTemplateOrThrow(templateId);

        // 防刷 1: 校验有效期
        if (!template.isValid(LocalDateTime.now())) {
            throw new BizException("400201", "券模板不在有效期内或已停用");
        }

        // 防刷 2: 校验总发放量
        if (template.isExhausted()) {
            throw new BizException("400202", "券已发完");
        }

        // 防刷 3: 校验每人限领数量
        int userCount = userCouponRepository.countByCustomerIdAndTemplateId(customerId, templateId);
        if (userCount >= template.getPerUserLimit()) {
            throw new BizException("400203",
                    "超过每人限领数量: " + template.getPerUserLimit());
        }

        // 发放
        template.issue();
        templateRepository.save(template);

        String id = generateId();
        String couponCode = generateCouponCode();
        UserCoupon userCoupon = UserCoupon.receive(id, couponCode, template, customerId);
        userCouponRepository.save(userCoupon);

        log.info("用户领券成功: customerId={}, couponCode={}, templateNo={}",
                customerId, couponCode, template.getTemplateNo());
        return UserCouponResult.from(userCoupon);
    }

    /**
     * 锁定券（下单时预占，防止重复使用，委托核销引擎）。
     */
    @Transactional
    public UserCouponResult lock(String couponCode, String orderId) {
        UserCoupon coupon = redemptionEngine.lock(couponCode, orderId);
        log.info("券已锁定: couponCode={}, orderId={}", couponCode, orderId);
        return UserCouponResult.from(coupon);
    }

    /**
     * 释放券（订单取消/超时未支付时回滚，委托核销引擎）。
     */
    @Transactional
    public UserCouponResult release(String couponCode) {
        UserCoupon coupon = redemptionEngine.release(couponCode);
        log.info("券已释放: couponCode={}", couponCode);
        return UserCouponResult.from(coupon);
    }

    /**
     * 核销券（订单支付成功后，委托核销引擎）。
     */
    @Transactional
    public UserCouponResult use(String couponCode, String orderId) {
        UserCoupon coupon = redemptionEngine.use(couponCode, orderId);
        log.info("券已核销: couponCode={}, orderId={}", couponCode, orderId);
        return UserCouponResult.from(coupon);
    }

    /**
     * 计算券可抵扣金额（试算，不修改状态，委托核销引擎）。
     *
     * @param couponCode  券码
     * @param orderAmount 订单金额（促销后）
     * @return 抵扣金额
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateDiscount(String couponCode, BigDecimal orderAmount) {
        return redemptionEngine.calculateDiscount(couponCode, orderAmount);
    }

    // ===== 查询 =====

    @Transactional(readOnly = true)
    public UserCouponResult getUserCoupon(String couponCode) {
        return UserCouponResult.from(loadUserCouponOrThrow(couponCode));
    }

    @Transactional(readOnly = true)
    public List<UserCouponResult> listByCustomer(String customerId) {
        return userCouponRepository.findByCustomerId(customerId).stream()
                .map(UserCouponResult::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserCouponResult> listByCustomerAndStatus(String customerId, String statusStr) {
        UserCouponStatus status = UserCouponStatus.valueOf(statusStr);
        return userCouponRepository.findByCustomerIdAndStatus(customerId, status).stream()
                .map(UserCouponResult::from)
                .toList();
    }

    // ===== 过期回收 =====

    /**
     * 过期券批量回收（供定时任务调用）。
     *
     * <p>将所有已过期但状态仍为 AVAILABLE/LOCKED 的券标记为 EXPIRED。</p>
     *
     * @return 回收的券数量
     */
    @Transactional
    public int expireCoupons() {
        List<UserCoupon> expired = userCouponRepository.findExpiredNotProcessed();
        for (UserCoupon coupon : expired) {
            coupon.expire();
            userCouponRepository.save(coupon);
        }
        if (!expired.isEmpty()) {
            log.info("过期券回收: count={}", expired.size());
        }
        return expired.size();
    }

    /**
     * 过期券每日定时回收（每日凌晨 2 点执行，配合启动类 @EnableScheduling）。
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void expireCouponsScheduled() {
        try {
            int count = expireCoupons();
            log.info("定时过期券回收完成: count={}", count);
        } catch (Exception e) {
            log.error("定时过期券回收失败", e);
        }
    }

    // ---------- 内部方法 ----------

    private CouponTemplate loadTemplateOrThrow(String templateId) {
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new BizException("404201", "券模板不存在: " + templateId));
    }

    private UserCoupon loadUserCouponOrThrow(String couponCode) {
        return userCouponRepository.findByCouponCode(couponCode)
                .orElseThrow(() -> new BizException("404202", "用户券不存在: " + couponCode));
    }

    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String generateTemplateNo() {
        return "CT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    private String generateCouponCode() {
        return "CP" + System.currentTimeMillis()
                + ThreadLocalRandom.current().nextInt(1000, 9999);
    }
}
