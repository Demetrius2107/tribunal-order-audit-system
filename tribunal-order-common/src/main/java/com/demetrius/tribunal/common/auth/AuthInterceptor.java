package com.demetrius.tribunal.common.auth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 认证授权拦截器（各业务服务注册，实现 JWT 鉴权 + RBAC 权限校验）。
 *
 * <p>处理链路（大厂网关/过滤器模式的服务端落点）：</p>
 * <ol>
 *   <li>读取 {@code Authorization: Bearer <token>} 请求头</li>
 *   <li>JwtTokenParser 本地解析校验（无效/过期 → 401）</li>
 *   <li>写入 UserContext（ThreadLocal，业务代码取当前用户）</li>
 *   <li>方法上有 {@link RequirePermission} → 比对权限，无权限 → 403</li>
 *   <li>afterCompletion 清理 ThreadLocal（防线程池复用串号）</li>
 * </ol>
 *
 * <p>白名单路径（login/register/心跳等）由注册方在 addInterceptors 里排除。</p>
 */
public class AuthInterceptor implements HandlerInterceptor {

    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenParser tokenParser;

    /** 内部调用令牌（服务间 Feign 调用凭据，配置 auth.internal-token） */
    private final String internalToken;

    public AuthInterceptor(JwtTokenParser tokenParser, String internalToken) {
        this.tokenParser = tokenParser;
        this.internalToken = internalToken;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 非 Controller 方法（如静态资源）直接放行
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 服务间 Feign 调用：X-Internal-Token 与本地配置一致 → 放行（记为内部用户）
        String internal = request.getHeader(FeignInternalTokenInterceptor.HEADER_INTERNAL_TOKEN);
        if (internalToken != null && !internalToken.isBlank() && internalToken.equals(internal)) {
            UserContext.set(new UserContext.CurrentUser("internal", "system", "SYSTEM", List.of()));
            return true;
        }

        String header = request.getHeader(HEADER_AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            writeError(response, 401, "未登录或 Token 缺失");
            return false;
        }

        String token = header.substring(BEARER_PREFIX.length());
        Claims claims = tokenParser.parseAccessToken(token);
        if (claims == null) {
            writeError(response, 401, "Token 无效或已过期");
            return false;
        }

        // 写入当前用户上下文
        UserContext.set(tokenParser.toCurrentUser(claims));

        // RBAC 权限校验（@RequirePermission 声明所需权限，任一满足即可）
        RequirePermission requirePermission = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (requirePermission != null && !hasAnyPermission(requirePermission.value())) {
            writeError(response, 403, "无权限执行该操作");
            return false;
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 必须清理：线程池复用线程，不清理会导致用户身份串号（越权漏洞）
        UserContext.clear();
    }

    private boolean hasAnyPermission(String[] required) {
        for (String perm : required) {
            if (UserContext.hasPermission(perm)) {
                return true;
            }
        }
        return false;
    }

    private void writeError(HttpServletResponse response, int status, String message) {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        try {
            response.getWriter().write(
                    "{\"success\":false,\"code\":\"" + status + "\",\"message\":\"" + message + "\",\"data\":null}");
        } catch (Exception ignored) {
            // 写失败不再处理
        }
    }
}