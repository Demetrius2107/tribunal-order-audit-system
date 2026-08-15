package com.demetrius.tribunal.marketing.application.service;

import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.marketing.application.dto.PromotionRuleResult;
import com.demetrius.tribunal.marketing.domain.model.PromotionRule;
import com.demetrius.tribunal.marketing.domain.model.PromotionTargetType;
import com.demetrius.tribunal.marketing.domain.model.PromotionType;
import com.demetrius.tribunal.marketing.domain.repository.PromotionRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 促销规则配置应用服务（F-201 促销配置化）。
 *
 * <p>提供规则的创建（草稿态 active=false）/ 上线 / 停用 / 查询，
 * 引擎通过 {@code findAllActive} 从 DB 读取启用中的规则，配置即生效。</p>
 */
@Service
public class PromotionRuleApplicationService {

    private static final Logger log = LoggerFactory.getLogger(PromotionRuleApplicationService.class);

    private final PromotionRuleRepository promotionRuleRepository;

    public PromotionRuleApplicationService(PromotionRuleRepository promotionRuleRepository) {
        this.promotionRuleRepository = promotionRuleRepository;
    }

    /**
     * 创建促销规则（初始 active=false 草稿态，不参与引擎计算）。
     */
    @Transactional
    public PromotionRuleResult createRule(String name, PromotionType type,
                                          PromotionTargetType targetType, String targetValue,
                                          BigDecimal threshold, BigDecimal discountRate,
                                          BigDecimal reductionAmount, BigDecimal halfPriceRate,
                                          String applicableSkuCode,
                                          String giftSkuCode, String giftSkuName, BigDecimal giftQuantity,
                                          boolean exclusive, int priority,
                                          LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new BizException("500002", "促销规则结束时间必须晚于开始时间");
        }
        String id = java.util.UUID.randomUUID().toString().replace("-", "");
        String ruleNo = "PROMO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(100, 1000);
        PromotionRule rule = new PromotionRule(
                id, ruleNo, name, type, targetType, targetValue,
                threshold, discountRate, reductionAmount, halfPriceRate, applicableSkuCode,
                giftSkuCode, giftSkuName, giftQuantity,
                exclusive, priority, false, startTime, endTime);
        promotionRuleRepository.save(rule);
        log.info("创建促销规则 ruleNo={} name={} type={}", ruleNo, name, type);
        return PromotionRuleResult.from(rule);
    }

    /**
     * 上线促销规则（active=true，引擎立即可命中）。
     */
    @Transactional
    public PromotionRuleResult activate(String ruleNo) {
        PromotionRule rule = findRequired(ruleNo);
        LocalDateTime now = LocalDateTime.now();
        if (rule.getStartTime() != null && now.isBefore(rule.getStartTime())) {
            throw new BizException("500003", "促销规则未到生效时间: " + rule.getStartTime());
        }
        // 构造器 active 为 final，通过重新创建同配置 + active=true 的规则实现状态切换
        PromotionRule activated = new PromotionRule(
                rule.getId(), rule.getRuleNo(), rule.getName(), rule.getType(), rule.getTargetType(), rule.getTargetValue(),
                rule.getThreshold(), rule.getDiscountRate(), rule.getReductionAmount(), rule.getHalfPriceRate(),
                rule.getApplicableSkuCode(),
                rule.getGiftSkuCode(), rule.getGiftSkuName(), rule.getGiftQuantity(),
                rule.isExclusive(), rule.getPriority(), true, rule.getStartTime(), rule.getEndTime());
        promotionRuleRepository.save(activated);
        log.info("上线促销规则 ruleNo={}", ruleNo);
        return PromotionRuleResult.from(activated);
    }

    /**
     * 停用促销规则（active=false，引擎立即不再命中）。
     */
    @Transactional
    public PromotionRuleResult deactivate(String ruleNo) {
        PromotionRule rule = findRequired(ruleNo);
        PromotionRule deactivated = new PromotionRule(
                rule.getId(), rule.getRuleNo(), rule.getName(), rule.getType(), rule.getTargetType(), rule.getTargetValue(),
                rule.getThreshold(), rule.getDiscountRate(), rule.getReductionAmount(), rule.getHalfPriceRate(),
                rule.getApplicableSkuCode(),
                rule.getGiftSkuCode(), rule.getGiftSkuName(), rule.getGiftQuantity(),
                rule.isExclusive(), rule.getPriority(), false, rule.getStartTime(), rule.getEndTime());
        promotionRuleRepository.save(deactivated);
        log.info("停用促销规则 ruleNo={}", ruleNo);
        return PromotionRuleResult.from(deactivated);
    }

    /**
     * 查询促销规则。
     */
    @Transactional(readOnly = true)
    public PromotionRuleResult getRule(String ruleNo) {
        return PromotionRuleResult.from(findRequired(ruleNo));
    }

    private PromotionRule findRequired(String ruleNo) {
        return promotionRuleRepository.findByRuleNo(ruleNo)
                .orElseThrow(() -> new BizException("500001", "促销规则不存在: " + ruleNo));
    }
}
