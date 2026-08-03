package com.demetrius.tribunal.notification.application.dto;

import com.demetrius.tribunal.notification.domain.model.NotificationMessage;
import com.demetrius.tribunal.notification.domain.model.NotificationType;

import java.time.LocalDateTime;

/**
 * 通知应用层出参。
 */
public record NotificationResult(
        String id,
        NotificationType type,
        String receiver,
        String title,
        String content,
        String status,
        LocalDateTime createdAt,
        LocalDateTime sentAt) {

    public static NotificationResult from(NotificationMessage message) {
        return new NotificationResult(
                message.getId(),
                message.getType(),
                message.getReceiver(),
                message.getTitle(),
                message.getContent(),
                message.getStatus(),
                message.getCreatedAt(),
                message.getSentAt());
    }
}
