package com.demetrius.tribunal.order.infrastructure.idempotency;

import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.common.redis.RedisIdempotencyGuard;
import com.demetrius.tribunal.order.domain.model.OrderSku;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * N-405 下单幂等守卫单测（Redis 分布式占位 + 本地降级）。
 */
class OrderIdempotencyGuardTest {

    private final RedisIdempotencyGuard redisGuard = mock(RedisIdempotencyGuard.class);
    private final OrderIdempotencyGuard guard = new OrderIdempotencyGuard(redisGuard);

    private List<OrderSku> skus() {
        return List.of(
                new OrderSku("SKU001", "商品A", new BigDecimal("10"), new BigDecimal("100.00")),
                new OrderSku("SKU002", "商品B", new BigDecimal("5"), new BigDecimal("200.00")));
    }

    @Test
    @DisplayName("正常提交：占位成功则放行")
    void checkDuplicateAllowsWhenAcquired() {
        when(redisGuard.tryAcquire(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Duration.class))).thenReturn(true);
        assertDoesNotThrow(() -> guard.checkDuplicate("cust-001", skus()));
    }

    @Test
    @DisplayName("重复提交：占位失败（窗口内命中）抛业务异常 200007")
    void checkDuplicateRejectsWhenNotAcquired() {
        when(redisGuard.tryAcquire(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Duration.class))).thenReturn(false);
        BizException ex = assertThrows(BizException.class, () -> guard.checkDuplicate("cust-001", skus()));
        assertEquals("200007", ex.getCode());
    }

    @Test
    @DisplayName("TTL 过期可重提：过期后占位成功则放行")
    void checkDuplicateAllowsAfterTtlExpiry() {
        // 第一次（窗口内）占位失败 → 拦截；TTL 过期后（模拟 Redis 键已删）占位成功 → 放行
        when(redisGuard.tryAcquire(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Duration.class)))
                .thenReturn(false) // 第一次调用：重复提交拦截
                .thenReturn(true); // 第二次调用：过期后重提放行
        assertThrows(BizException.class, () -> guard.checkDuplicate("cust-001", skus()));
        assertDoesNotThrow(() -> guard.checkDuplicate("cust-001", skus()));
    }
}
