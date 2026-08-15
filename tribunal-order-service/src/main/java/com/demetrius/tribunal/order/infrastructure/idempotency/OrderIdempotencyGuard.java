package com.demetrius.tribunal.order.infrastructure.idempotency;

import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.common.redis.RedisIdempotencyGuard;
import com.demetrius.tribunal.order.domain.model.OrderSku;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 下单幂等守卫（同客户 + 同明细短时间重复提交拦截，N-405）。
 *
 * <p>Redis 分布式实现（M5）：key = 客户ID + 明细指纹，SETNX + TTL 30s 原子占位；
 * 命中窗口内重复提交 → 抛 {@code 200007}。Redis 不可用时由
 * {@link RedisIdempotencyGuard} 自动降级到本地缓存（单实例环境仍有效）。</p>
 *
 * <p>替换说明：原进程内 ConcurrentHashMap 仅单实例有效，多实例会重复下单；
 * 本次改造保留类名与方法签名（调用点不变），内部切换为分布式守卫。</p>
 */
@Component
public class OrderIdempotencyGuard {

    /** 防重窗口：30 秒内同客户同明细视为重复提交 */
    private static final Duration TTL = Duration.ofSeconds(30);

    private final RedisIdempotencyGuard redisIdempotencyGuard;

    public OrderIdempotencyGuard(RedisIdempotencyGuard redisIdempotencyGuard) {
        this.redisIdempotencyGuard = redisIdempotencyGuard;
    }

    /**
     * 校验是否重复提交：窗口内命中则抛业务异常。
     *
     * <p>SETNX 原子占位：占位成功（首次提交）放行；占位失败（窗口内重复）拦截；
     * TTL 30s 到期后 Redis 自动删键，允许再次提交。</p>
     *
     * @param customerId 客户 ID
     * @param skus       订单明细
     */
    public void checkDuplicate(String customerId, List<OrderSku> skus) {
        String key = buildKey(customerId, skus);
        boolean acquired = redisIdempotencyGuard.tryAcquire(key, TTL);
        if (!acquired) {
            throw new BizException("200007", "请勿重复提交订单（同客户同明细 30 秒内）");
        }
    }

    /**
     * 幂等 key = 客户ID + 明细指纹（skuCode:quantity:price 按 skuCode 排序拼接）。
     */
    private String buildKey(String customerId, List<OrderSku> skus) {
        String fingerprint = skus.stream()
                .sorted(Comparator.comparing(OrderSku::getSkuCode))
                .map(s -> s.getSkuCode() + ":" + s.getQuantity() + ":" + s.getPrice())
                .collect(Collectors.joining("|"));
        return customerId + "#" + fingerprint;
    }
}
