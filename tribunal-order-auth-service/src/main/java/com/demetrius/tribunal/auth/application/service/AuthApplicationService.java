package com.demetrius.tribunal.auth.application.service;

import com.demetrius.tribunal.auth.application.dto.AuthResult;
import com.demetrius.tribunal.auth.application.dto.LoginCommand;
import com.demetrius.tribunal.auth.application.dto.TokenValidationResult;
import com.demetrius.tribunal.auth.domain.model.Permission;
import com.demetrius.tribunal.auth.domain.model.User;
import com.demetrius.tribunal.auth.domain.repository.PermissionRepository;
import com.demetrius.tribunal.auth.domain.repository.UserRepository;
import com.demetrius.tribunal.common.exception.BizException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 认证授权应用服务（登录/注册/刷新/登出/Token 校验，RBAC 权限注入）。
 *
 * <p>对应需求：F-901（用户/角色/权限）、N-401（认证）、N-402（授权）。</p>
 *
 * <p>大厂链路：登录校验 → 失败锁定防爆破 → 签发 Access+Refresh 双 Token，
 * Access Token 携带角色权限 claims（下游无状态鉴权），Refresh Token 轮换防重放。</p>
 */
@Service
public class AuthApplicationService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final JwtProvider jwtProvider;
    private final LoginFailGuard loginFailGuard;
    private final RefreshTokenStore refreshTokenStore;

    public AuthApplicationService(UserRepository userRepository,
                                  PermissionRepository permissionRepository,
                                  JwtProvider jwtProvider,
                                  LoginFailGuard loginFailGuard,
                                  RefreshTokenStore refreshTokenStore) {
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
        this.jwtProvider = jwtProvider;
        this.loginFailGuard = loginFailGuard;
        this.refreshTokenStore = refreshTokenStore;
    }

    /**
     * 登录：校验凭据（含失败锁定防爆破）→ 查角色权限 → 签发 Access + Refresh 双 Token。
     */
    @Transactional(readOnly = true)
    public AuthResult login(LoginCommand command) {
        if (loginFailGuard.isLocked(command.username())) {
            throw new BizException("800004", "登录失败次数过多，账号已锁定，请稍后再试");
        }

        User user = userRepository.findByUsername(command.username())
                .orElseThrow(() -> {
                    loginFailGuard.recordFailure(command.username());
                    return new BizException("800001", "用户不存在或密码错误");
                });

        if (!user.verifyPassword(command.password())) {
            boolean locked = loginFailGuard.recordFailure(command.username());
            throw new BizException(locked ? "800004" : "800002",
                    locked ? "登录失败次数过多，账号已锁定，请稍后再试" : "用户不存在或密码错误");
        }

        loginFailGuard.reset(command.username());
        return issueTokens(user);
    }

    /**
     * 注册：创建用户（BCrypt 加密密码）→ 签发双 Token。
     */
    @Transactional
    public AuthResult register(String username, String password, String roleCode) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new BizException("800003", "用户名已存在");
        }
        User user = new User(UUID.randomUUID().toString().replace("-", ""),
                username, password, roleCode);
        userRepository.save(user);
        return issueTokens(user);
    }

    /**
     * 刷新：校验 Refresh Token（轮换）→ 签发新的双 Token（旧 refresh 作废，防重放）。
     */
    @Transactional(readOnly = true)
    public AuthResult refresh(String refreshToken) {
        String userId = refreshTokenStore.getUserId(refreshToken);
        if (userId == null) {
            throw new BizException("800005", "Refresh Token 无效或已过期，请重新登录");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException("800001", "用户不存在"));
        // 轮换：issue() 内部先吊销该用户旧 token
        return issueTokens(user);
    }

    /**
     * 登出：吊销该用户全部 Refresh Token（Access Token 靠过期自然失效）。
     */
    @Transactional(readOnly = true)
    public void logout(String refreshToken) {
        refreshTokenStore.revoke(refreshToken);
    }

    /**
     * 校验 Token 有效性（供 API 网关/过滤器/RBAC 鉴权调用）。
     */
    @Transactional(readOnly = true)
    public TokenValidationResult validateToken(String token) {
        try {
            Claims claims = jwtProvider.validateTokenOfType(token, JwtProvider.TYPE_ACCESS);
            return new TokenValidationResult(true, false,
                    claims.getSubject(),
                    claims.get(JwtProvider.CLAIM_USERNAME, String.class),
                    claims.get(JwtProvider.CLAIM_ROLE_CODE, String.class),
                    jwtProvider.getPermissions(claims));
        } catch (ExpiredJwtException e) {
            Claims claims = e.getClaims();
            return new TokenValidationResult(false, true,
                    claims.getSubject(),
                    claims.get(JwtProvider.CLAIM_USERNAME, String.class),
                    claims.get(JwtProvider.CLAIM_ROLE_CODE, String.class),
                    jwtProvider.getPermissions(claims));
        } catch (JwtException | IllegalArgumentException e) {
            return new TokenValidationResult(false, false, null, null, null, List.of());
        }
    }

    /**
     * 按角色查询权限点列表（RBAC 权限管理）。
     */
    @Transactional(readOnly = true)
    public List<Permission> getPermissionsByRole(String roleCode) {
        return permissionRepository.findByRoleCode(roleCode);
    }

    // ---------- private ----------

    /**
     * 统一签发双 Token：查角色权限 → Access(含权限) + Refresh(轮换)。
     */
    private AuthResult issueTokens(User user) {
        List<String> permissions = permissionRepository.findByRoleCode(user.getRoleCode())
                .stream().map(Permission::getPermissionCode).toList();

        String accessToken = jwtProvider.generateAccessToken(
                user.getId(), user.getUsername(), user.getRoleCode(), permissions);
        String refreshToken = refreshTokenStore.issue(
                user.getId(), user.getUsername(), user.getRoleCode());
        return new AuthResult(accessToken, refreshToken,
                user.getId(), user.getUsername(), user.getRoleCode(), permissions);
    }
}