package com.demetrius.tribunal.notification.infrastructure.repository;

import com.demetrius.tribunal.notification.domain.model.NotificationMessage;
import com.demetrius.tribunal.notification.domain.model.NotificationType;
import com.demetrius.tribunal.notification.domain.repository.NotificationMessageRepository;
import com.demetrius.tribunal.notification.infrastructure.mapper.NotificationMessageMapper;
import com.demetrius.tribunal.notification.infrastructure.model.NotificationMessagePo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 通知消息仓储实现（MyBatis-Plus）。
 */
@Repository
public class NotificationMessageRepositoryImpl implements NotificationMessageRepository {

    private final NotificationMessageMapper notificationMessageMapper;

    public NotificationMessageRepositoryImpl(NotificationMessageMapper notificationMessageMapper) {
        this.notificationMessageMapper = notificationMessageMapper;
    }

    @Override
    public void save(NotificationMessage message) {
        NotificationMessagePo po = toPo(message);
        NotificationMessagePo exist = notificationMessageMapper.selectById(message.getId());
        if (exist == null) {
            notificationMessageMapper.insert(po);
        } else {
            notificationMessageMapper.updateById(po);
        }
    }

    @Override
    public Optional<NotificationMessage> findById(String id) {
        NotificationMessagePo po = notificationMessageMapper.selectById(id);
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    // ---------- 转换方法（PO ↔ 领域对象） ----------

    private NotificationMessage toDomain(NotificationMessagePo po) {
        return new NotificationMessage(
                po.getId(),
                NotificationType.valueOf(po.getType()),
                po.getReceiver(),
                po.getTitle(),
                po.getContent());
    }

    private NotificationMessagePo toPo(NotificationMessage message) {
        NotificationMessagePo po = new NotificationMessagePo();
        po.setId(message.getId());
        po.setType(message.getType().name());
        po.setReceiver(message.getReceiver());
        po.setTitle(message.getTitle());
        po.setContent(message.getContent());
        po.setStatus(message.getStatus());
        po.setCreatedAt(message.getCreatedAt());
        po.setSentAt(message.getSentAt());
        return po;
    }
}
