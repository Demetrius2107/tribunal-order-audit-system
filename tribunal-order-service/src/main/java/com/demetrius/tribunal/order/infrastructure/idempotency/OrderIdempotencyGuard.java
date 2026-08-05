package com.demetrius.tribunal.order.infrastructure.idempotency;

import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.order.domain.model.OrderSku;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 下单幂等守卫（同客户 + 同明细短时间重复提交拦截，N-405）。
 *
 * <p>骨架说明：当前用进程内 ConcurrentHashMap 实现（单实例有效）；多实例/集群场景
 * 需替换为 Redis（key = 客户ID+明细指纹，TTL=30s），M4 里程碑接入。</p>
 */
@Component
public class OrderIdempotencyGuard {

    /** 防重窗口：30 秒内同客户同明细视为重复提交 */
    private static final long TTL_MILLIS = 30_000L;

    /** key → 首次提交时间戳 */
    private final Map<String, Long> submitted = new ConcurrentHashMap<>();

    /**
     * 校验是否重复提交：窗口内命中则抛业务异常。
     *
     * @param customerId 客户 ID
     * @param skus       订单明细
     */
    public void checkDuplicate(String customerId, List<OrderSku> skus) {
        String key = buildKey(customerId, skus);
        long now = System.currentTimeMillis();
        Long first = submitted.putIfAbsent(key, now);
        if (first != null && now - first < TTL_MILLIS) {
            throw new BizException("200007", "请勿重复提交订单（同客户同明细 30 秒内）");
        }
        // 更新提交时间并顺带清理过期 key（避免 Map 无限增长）
        submitted.put(key, now);
        submitted.entrySet().removeIf(e -> now - e.getValue() > TTL_MILLIS);
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
