package com.demetrius.tribunal.auth.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.auth.domain.model.User;
import com.demetrius.tribunal.auth.domain.repository.UserRepository;
import com.demetrius.tribunal.auth.infrastructure.mapper.UserMapper;
import com.demetrius.tribunal.auth.infrastructure.model.UserPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户仓储实现（MyBatis-Plus）。
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;

    public UserRepositoryImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public void save(User user) {
        UserPo po = toPo(user);
        userMapper.insert(po);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        UserPo po = userMapper.selectOne(
                new LambdaQueryWrapper<UserPo>().eq(UserPo::getUsername, username));
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public Optional<User> findById(String id) {
        UserPo po = userMapper.selectById(id);
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    // ---------- 转换方法（PO ↔ 领域对象） ----------

    private User toDomain(UserPo po) {
        return new User(po.getId(), po.getUsername(), po.getPassword(), po.getRoleCode());
    }

    private UserPo toPo(User user) {
        UserPo po = new UserPo();
        po.setId(user.getId());
        po.setUsername(user.getUsername());
        po.setPassword(user.getPassword());
        po.setRoleCode(user.getRoleCode());
        return po;
    }
}
