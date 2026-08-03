package com.demetrius.tribunal.auth.application.service;

import com.demetrius.tribunal.auth.application.dto.AuthResult;
import com.demetrius.tribunal.auth.application.dto.LoginCommand;
import com.demetrius.tribunal.auth.domain.model.User;
import com.demetrius.tribunal.auth.domain.repository.UserRepository;
import com.demetrius.tribunal.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 认证授权应用服务（登录/注册用例）。
 *
 * <p>对应需求：F-901（用户/角色/权限）、N-401（认证）、N-402（授权）。</p>
 *
 * <p>职责：</p>
 * <ol>
 *   <li>登录：校验用户名密码 → 签发 Token</li>
 *   <li>注册：创建用户（TODO：密码 BCrypt 加密）</li>
 * </ol>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>JWT 签发/校验（jjwt 依赖，token 过期与刷新）——对照 N-401</li>
 *   <li>RBAC：角色-权限表 + 接口级鉴权过滤器——对照 N-402</li>
 *   <li>登录失败计数/锁定（防爆破）</li>
 * </ul>
 */
@Service
public class AuthApplicationService {

    private final UserRepository userRepository;

    public AuthApplicationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 登录：校验凭据，签发 Token（骨架：UUID token；TODO：JWT）。
     */
    @Transactional(readOnly = true)
    public AuthResult login(LoginCommand command) {
        User user = userRepository.findByUsername(command.username())
                .orElseThrow(() -> new BizException("800001", "用户不存在或密码错误"));

        if (!user.verifyPassword(command.password())) {
            throw new BizException("800002", "用户不存在或密码错误");
        }

        // TODO（学习任务）：签发 JWT（含过期时间/角色声明），此处先用 UUID 占位
        String token = UUID.randomUUID().toString().replace("-", "");
        return new AuthResult(token, user.getId(), user.getUsername(), user.getRoleCode());
    }

    /**
     * 注册（骨架：明文密码入库；TODO：BCrypt 加密）。
     */
    @Transactional
    public AuthResult register(String username, String password, String roleCode) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new BizException("800003", "用户名已存在");
        }
        User user = new User(UUID.randomUUID().toString().replace("-", ""),
                username, password, roleCode);
        userRepository.save(user);
        return new AuthResult(null, user.getId(), user.getUsername(), user.getRoleCode());
    }
}
