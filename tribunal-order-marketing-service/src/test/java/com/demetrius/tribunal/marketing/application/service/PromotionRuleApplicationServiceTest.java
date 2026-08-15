package com.demetrius.tribunal.marketing.application.service;

import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.marketing.application.dto.PromotionRuleResult;
import com.demetrius.tribunal.marketing.domain.model.PromotionRule;
import com.demetrius.tribunal.marketing.domain.model.PromotionTargetType;
import com.demetrius.tribunal.marketing.domain.model.PromotionType;
import com.demetrius.tribunal.marketing.domain.repository.PromotionRuleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * F-201 促销规则配置应用服务单测（创建草稿态/上线/停用/查询/生效期校验）。
 */
class PromotionRuleApplicationServiceTest {

    private final PromotionRuleRepository repo = mock(PromotionRuleRepository.class);
    private final PromotionRuleApplicationService service = new PromotionRuleApplicationService(repo);

    /** 记录仓储最近一次保存的规则（模拟 DB 落库）。 */
    private PromotionRule savedRule;

    private void stubSaveToCapture() {
        doAnswer(inv -> {
            savedRule = inv.getArgument(0);
            return null;
        }).when(repo).save(any(PromotionRule.class));
    }

    private PromotionRule activeRule() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        return new PromotionRule(
                "r-001", "PROMO001", "满100减10", PromotionType.FULL_REDUCTION,
                PromotionTargetType.ALL, null,
                new BigDecimal("100"), null, new BigDecimal("10"), null, null,
                null, null, null, false, 1, true, start, end);
    }

    @Test
    @DisplayName("创建规则：ruleNo 自动生成，初始草稿态（active=false）")
    void createRuleIsDraft() {
        stubSaveToCapture();
        PromotionRuleResult result = service.createRule(
                "满100减10", PromotionType.FULL_REDUCTION, PromotionTargetType.ALL, null,
                new BigDecimal("100"), null, new BigDecimal("10"), null, null,
                null, null, null, false, 1,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        assertNotNull(result.ruleNo());
        assertTrue(result.ruleNo().startsWith("PROMO"));
        assertFalse(result.active(), "新建规则应为草稿态（不参与引擎计算）");
        assertNotNull(savedRule);
    }

    @Test
    @DisplayName("创建规则：结束时间早于开始时间被拒")
    void createRuleRejectsInvalidWindow() {
        assertThrows(BizException.class, () -> service.createRule(
                "非法活动", PromotionType.DISCOUNT, PromotionTargetType.ALL, null,
                null, new BigDecimal("0.9"), null, null, null,
                null, null, null, false, 1,
                LocalDateTime.now().plusDays(1), LocalDateTime.now()));
    }

    @Test
    @DisplayName("上线规则：active 变 true（引擎立即可命中）")
    void activateRuleEnables() {
        stubSaveToCapture();
        when(repo.findByRuleNo("PROMO001")).thenReturn(Optional.of(activeRule()));

        PromotionRuleResult result = service.activate("PROMO001");
        assertTrue(result.active());
        assertTrue(savedRule.isActive());
    }

    @Test
    @DisplayName("上线规则：未到生效时间被拒（业务码 500003）")
    void activateRuleBeforeStartRejected() {
        LocalDateTime start = LocalDateTime.now().plusDays(1); // 明天才生效
        LocalDateTime end = LocalDateTime.now().plusDays(2);
        PromotionRule future = new PromotionRule(
                "r-002", "PROMO002", "未来活动", PromotionType.DISCOUNT,
                PromotionTargetType.ALL, null,
                null, new BigDecimal("0.9"), null, null, null,
                null, null, null, false, 1, false, start, end);
        when(repo.findByRuleNo("PROMO002")).thenReturn(Optional.of(future));

        BizException ex = assertThrows(BizException.class, () -> service.activate("PROMO002"));
        assertEquals("500003", ex.getCode());
    }

    @Test
    @DisplayName("停用规则：active 变 false（引擎立即不再命中）")
    void deactivateRuleDisables() {
        stubSaveToCapture();
        when(repo.findByRuleNo("PROMO001")).thenReturn(Optional.of(activeRule()));

        PromotionRuleResult result = service.deactivate("PROMO001");
        assertFalse(result.active());
        assertFalse(savedRule.isActive());
    }

    @Test
    @DisplayName("查询规则：不存在抛业务码 500001")
    void getRuleNotFound() {
        when(repo.findByRuleNo("PROMO-NOT-EXIST")).thenReturn(Optional.empty());
        BizException ex = assertThrows(BizException.class, () -> service.getRule("PROMO-NOT-EXIST"));
        assertEquals("500001", ex.getCode());
    }

    @Test
    @DisplayName("查询规则：返回完整配置")
    void getRuleReturnsConfig() {
        when(repo.findByRuleNo("PROMO001")).thenReturn(Optional.of(activeRule()));
        PromotionRuleResult result = service.getRule("PROMO001");
        assertEquals("PROMO001", result.ruleNo());
        assertEquals("FULL_REDUCTION", result.type());
        assertEquals("ALL", result.targetType());
        assertEquals(0, new BigDecimal("100").compareTo(result.threshold()));
        assertEquals(0, new BigDecimal("10").compareTo(result.reductionAmount()));
    }
}
