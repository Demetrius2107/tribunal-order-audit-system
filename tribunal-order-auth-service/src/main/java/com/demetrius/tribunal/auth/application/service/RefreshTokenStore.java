package com.demetrius.tribunal.auth.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Refresh Token 存储（轮换 + 吊销）。
 *
 * <p>大厂做法：Refresh Token 有状态管理（Redis/DB），支持轮换（每次刷新旧 token 立即作废，防重放）与吊销（登出即失效）。
 * 单实例内存实现（学习项目）；生产环境应换 Redis（key=refreshToken, value=userId, TTL=refresh 有效期）。</p>
 */
@Component
public class RefreshTokenStore {

    private static final class TokenEntry {
        String userId;
        String username;
        String roleCode;
        LocalDateTime expiresAt;
    }

    /** refreshToken -> 用户身份 */
    private final ConcurrentHashMap<String, TokenEntry> store = new ConcurrentHashMap<>();

    private final long refreshTtlMs;

    public RefreshTokenStore(@Value("${jwt.refresh-expiration-ms:604800000}") long refreshTtlMs) {
        this.refreshTtlMs = refreshTtlMs;
    }

    /**
     * 生成并保存一个新的 Refresh Token（轮换：同一用户旧 token 全部作废）。
     */
    public String issue(String userId, String username, String roleCode) {
        // 轮换：先吊销该用户所有旧 refresh token，防止重放（大厂标准做法）
        revokeByUser(userId);

        String token = UUID.randomUUID().toString().replace("-", "");
        TokenEntry entry = new TokenEntry();
        entry.userId = userId;
        entry.username = username;
        entry.roleCode = roleCode;
        entry.expiresAt = LocalDateTime.now().plusNanos(refreshTtlMs * 1_000_000L);
        store.put(token, entry);
        return token;
    }

    /**
     * 校验并取出用户身份（校验失败/过期返回 null）。
     */
    public TokenEntry validateAndGet(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return null;
        }
        TokenEntry entry = store.get(refreshToken.trim());
        if (entry == null) {
            return null;
        }
        if (LocalDateTime.now().isAfter(entry.expiresAt)) {
            store.remove(refreshToken);
            return null;
        }
        return entry;
    }

    /**
     * 吊销指定 token（登出）。
     */
    public void revoke(String refreshToken) {
        if (refreshToken != null) {
            store.remove(refreshToken.trim());
        }
    }

    /**
     * 吊销某用户全部 refresh token（轮换/踢下线）。
     */
    public void revokeByUser(String userId) {
        store.entrySet().removeIf(e -> e.getValue().userId.equals(userId));
    }

    /**
     * 当前有效的 token 数（运维/统计用）。
     */
    public int activeCount() {
        // 清理过期项后返回数量
        store.entrySet().removeIf(e -> LocalDateTime.now().isAfter(e.getValue().expiresAt));
        return store.size();
    }

    public String getUserId(String refreshToken) {
        TokenEntry entry = validateAndGet(refreshToken);
        return entry == null ? null : entry.userId;
    }

    public String getUsername(String refreshToken) {
        TokenEntry entry = validateAndGet(refreshToken);
        return entry == null ? null : entry.username;
    }

    public String getRoleCode(String refreshToken) {
        TokenEntry entry = validateAndGet(refreshToken);
        return entry == null ? null : entry.roleCode;
    }
}