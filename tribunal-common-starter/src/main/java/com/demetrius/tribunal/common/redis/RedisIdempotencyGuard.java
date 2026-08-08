package com.demetrius.tribunal.common.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 分布式幂等守卫（M5 Redis 实现 + 本地降级）。
 *
 * <p>用途：</p>
 * <ul>
 *   <li>订单防重提交：同客户同明细在时间窗口内不允许重复下单</li>
 *   <li>接口幂等：同一个 requestId 只处理一次</li>
 *   <li>登录失败锁定：连续失败 N 次后锁定账号一段时间</li>
 * </ul>
 *
 * <p>降级策略：Redis 不可用时自动降级到本地 ConcurrentHashMap，
 * 单实例环境仍可用（测试/开发），多实例环境会弱化（无法跨实例幂等）。</p>
 */
public class RedisIdempotencyGuard {

    private static final Logger log = LoggerFactory.getLogger(RedisIdempotencyGuard.class);

    /** 本地降级缓存：key → 过期时间戳 */
    private final ConcurrentMap<String, Long> localFallback = new ConcurrentHashMap<>();

    private final StringRedisTemplate redisTemplate;
    private final String keyPrefix;

    public RedisIdempotencyGuard(StringRedisTemplate redisTemplate, String keyPrefix) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix;
    }

    /**
     * 尝试占位（SETNX 语义）。
     *
     * @param key      业务键（如 customerId + skuHash）
     * @param ttl      过期时间
     * @return true=占位成功（可继续处理），false=已存在（重复请求）
     */
    public boolean tryAcquire(String key, Duration ttl) {
        String redisKey = keyPrefix + ":" + key;
        if (redisTemplate != null) {
            try {
                Boolean acquired = redisTemplate.opsForValue()
                        .setIfAbsent(redisKey, "1", ttl);
                return Boolean.TRUE.equals(acquired);
            } catch (Exception ex) {
                log.warn("Redis 幂等检查失败，降级到本地: {}", ex.getMessage());
            }
        }
        // 本地降级
        long now = System.currentTimeMillis();
        long expireAt = now + ttl.toMillis();
        localFallback.values().removeIf(expiry -> expiry < now);
        Long existing = localFallback.putIfAbsent(redisKey, expireAt);
        return existing == null;
    }

    /**
     * 原子递增 + 读取（用于登录失败计数）。
     *
     * @param key 业务键
     * @param ttl 过期时间（首次创建时生效）
     * @return 当前计数
     */
    public long incrementAndGet(String key, Duration ttl) {
        String redisKey = keyPrefix + ":" + key;
        if (redisTemplate != null) {
            try {
                Long count = redisTemplate.opsForValue().increment(redisKey);
                if (count != null && count == 1L) {
                    redisTemplate.expire(redisKey, ttl);
                }
                return count != null ? count : 1L;
            } catch (Exception ex) {
                log.warn("Redis increment 失败，降级到本地: {}", ex.getMessage());
            }
        }
        // 本地降级（简化：直接返回递增值，过期清理由 tryAcquire 路径触发）
        long now = System.currentTimeMillis();
        localFallback.values().removeIf(expiry -> expiry < now);
        String counterKey = redisKey + ":counter";
        long current = Long.parseLong(
                String.valueOf(localFallback.getOrDefault(counterKey, 0L)));
        current++;
        localFallback.put(counterKey, current);
        return current;
    }

    /**
     * 删除键（业务成功后可选清理）。
     */
    public void release(String key) {
        String redisKey = keyPrefix + ":" + key;
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(redisKey);
                return;
            } catch (Exception ex) {
                log.warn("Redis delete 失败: {}", ex.getMessage());
            }
        }
        localFallback.remove(redisKey);
    }

    /**
     * 是否可用 Redis（true=远程，false=本地降级）。
     */
    public boolean isRedisAvailable() {
        return redisTemplate != null;
    }
}
