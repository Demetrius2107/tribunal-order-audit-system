package com.demetrius.tribunal.common.auth;

import java.util.List;

/**
 * 当前登录用户上下文（ThreadLocal）。
 *
 * <p>由 {@link AuthInterceptor} 在请求进入时写入、请求结束时清理。
 * 业务代码通过 {@link #getRequiredUserId()} 获取当前操作用户，避免每个接口手动传参。</p>
 *
 * <p><b>必须清理</b>：Web 容器线程池会复用线程，若不清理会导致串号
 * （A 用户请求后线程被 B 用户复用，B 误读到 A 的身份）——订单系统最致命的越权漏洞。</p>
 */
public final class UserContext {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public record CurrentUser(
            String userId,
            String username,
            String roleCode,
            List<String> permissions) {
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    /**
     * 获取当前用户 ID；未登录（无上下文）时返回 null。
     */
    public static String getUserId() {
        CurrentUser user = HOLDER.get();
        return user == null ? null : user.userId();
    }

    /**
     * 获取当前用户 ID；未登录直接抛异常（用于必须登录的接口）。
     */
    public static String getRequiredUserId() {
        CurrentUser user = HOLDER.get();
        if (user == null || user.userId() == null) {
            throw new IllegalStateException("当前请求未登录或登录上下文已失效");
        }
        return user.userId();
    }

    /**
     * 是否拥有指定权限码（RBAC）。
     */
    public static boolean hasPermission(String permissionCode) {
        CurrentUser user = HOLDER.get();
        return user != null && user.permissions() != null
                && user.permissions().contains(permissionCode);
    }

    /**
     * 请求结束后清理（由拦截器 afterCompletion 调用）。
     */
    public static void clear() {
        HOLDER.remove();
    }
}