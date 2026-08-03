package com.demetrius.tribunal.notification.domain.repository;

import com.demetrius.tribunal.notification.domain.model.NotificationMessage;

import java.util.Optional;

/**
 * 通知消息仓储接口。
 */
public interface NotificationMessageRepository {

    void save(NotificationMessage message);

    Optional<NotificationMessage> findById(String id);
}
