package com.demetrius.tribunal.auth.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败锁定守卫（防暴力破解，N-401 深化）。
 *
 * <p>策略：同一用户名连续失败 {@code maxFailures} 次后锁定 {@code lockMinutes} 分钟。
 * 单实例内存实现（学习项目）；生产环境应换 Redis（INCR + TTL），支持多实例共享锁定状态。</p>
 */
@Component
public class LoginFailGuard {

    private static final class FailState {
        int count;
        LocalDateTime lockUntil;
    }

    private final ConcurrentHashMap<String, FailState> states = new ConcurrentHashMap<>();

    private final int maxFailures;
    private final long lockMinutes;

    public LoginFailGuard(@Value("${jwt.login.max-failures:5}") int maxFailures,
                          @Value("${jwt.login.lock-minutes:15}") long lockMinutes) {
        this.maxFailures = maxFailures;
        this.lockMinutes = lockMinutes;
    }

    /**
     * 校验该用户名当前是否被锁定。
     */
    public boolean isLocked(String username) {
        FailState state = states.get(username);
        if (state == null || state.lockUntil == null) {
            return false;
        }
        // 锁定已过期 → 清除并放行
        if (LocalDateTime.now().isAfter(state.lockUntil)) {
            states.remove(username);
            return false;
        }
        return true;
    }

    /**
     * 记录一次登录失败；返回是否已达到锁定阈值。
     */
    public boolean recordFailure(String username) {
        FailState state = states.computeIfAbsent(username, k -> new FailState());
        synchronized (state) {
            if (isLocked(username)) {
                return true;
            }
            state.count++;
            if (state.count >= maxFailures) {
                state.lockUntil = LocalDateTime.now().plusMinutes(lockMinutes);
                state.count = 0;
                return true;
            }
            return false;
        }
    }

    /**
     * 登录成功后清除失败计数。
     */
    public void reset(String username) {
        states.remove(username);
    }

    /**
     * 剩余可用失败次数（未锁定时）。
     */
    public int remainingAttempts(String username) {
        FailState state = states.get(username);
        if (state == null || state.lockUntil != null) {
            return maxFailures;
        }
        return Math.max(0, maxFailures - state.count);
    }
}