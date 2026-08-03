package com.demetrius.tribunal.auth.domain.repository;

import com.demetrius.tribunal.auth.domain.model.User;

import java.util.Optional;

/**
 * 用户仓储接口。
 */
public interface UserRepository {

    void save(User user);

    Optional<User> findByUsername(String username);

    Optional<User> findById(String id);
}
