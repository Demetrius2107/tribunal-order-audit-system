package com.demetrius.tribunal.notification.application.dto;

import com.demetrius.tribunal.notification.domain.model.NotificationType;

/**
 * 通知发送入参。
 */
public record NotificationSendCommand(
        NotificationType type,
        String receiver,
        String title,
        String content) {
}
