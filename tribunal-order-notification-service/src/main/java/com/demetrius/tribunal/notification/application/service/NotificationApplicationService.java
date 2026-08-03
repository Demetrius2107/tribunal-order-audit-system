package com.demetrius.tribunal.notification.application.service;

import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.notification.application.dto.NotificationSendCommand;
import com.demetrius.tribunal.notification.application.dto.NotificationResult;
import com.demetrius.tribunal.notification.domain.model.NotificationMessage;
import com.demetrius.tribunal.notification.domain.repository.NotificationMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 通知应用服务（用例编排层）。
 *
 * <p>对应需求：F-701（站内信）、F-702（邮件）、F-703（短信/微信）。</p>
 *
 * <p>职责：</p>
 * <ol>
 *   <li>创建并发送通知（站内信/邮件/短信）</li>
 *   <li>记录发送状态（PENDING → SENT/FAILED）</li>
 * </ol>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>模板渲染：模板编码 + 占位符 → 内容</li>
 *   <li>渠道 SDK 对接（邮件/短信/微信）</li>
 *   <li>失败重试与死信（消息不丢，N-305）</li>
 * </ul>
 */
@Service
public class NotificationApplicationService {

    private final NotificationMessageRepository notificationMessageRepository;

    public NotificationApplicationService(NotificationMessageRepository notificationMessageRepository) {
        this.notificationMessageRepository = notificationMessageRepository;
    }

    /**
     * 发送通知（骨架：落库 + 标记已发送；渠道对接留 TODO）。
     */
    @Transactional
    public NotificationResult send(NotificationSendCommand command) {
        NotificationMessage message = new NotificationMessage(
                generateId(),
                command.type(),
                command.receiver(),
                command.title(),
                command.content());

        try {
            // TODO（学习任务）：按 type 分发到渠道适配器（邮件/短信/微信 SDK）
            message.markSent();
        } catch (Exception e) {
            message.markFailed();
            // TODO（学习任务）：失败重试/死信
        }
        notificationMessageRepository.save(message);
        return NotificationResult.from(message);
    }

    /**
     * 查询通知。
     */
    @Transactional(readOnly = true)
    public NotificationResult get(String id) {
        return NotificationResult.from(notificationMessageRepository.findById(id)
                .orElseThrow(() -> new BizException("700001", "通知不存在: " + id)));
    }

    private String generateId() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
}
