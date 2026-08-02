package com.demetrius.tribunal.notification.domain.model;

import java.time.LocalDateTime;

/**
 * 通知消息聚合根。
 *
 * <p>对应需求：F-701（站内信）、F-702（邮件）、F-703（短信/微信）、F-704（模板管理）。</p>
 *
 * <p>职责：承载一条通知（类型/接收人/标题/内容/模板），记录发送状态与重试。</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>模板管理：模板编码 → 内容（占位符渲染）</li>
 *   <li>发送渠道适配：邮件/短信/微信 SDK 对接</li>
 *   <li>失败重试与死信（消息不丢，N-305）</li>
 * </ul>
 */
public class NotificationMessage {

    private final String id;

    private final NotificationType type;

    /** 接收人（用户ID/邮箱/手机号） */
    private final String receiver;

    private final String title;

    private final String content;

    /** 发送状态：PENDING/SENT/FAILED */
    private String status;

    private final LocalDateTime createdAt;

    private LocalDateTime sentAt;

    public NotificationMessage(String id, NotificationType type, String receiver,
                               String title, String content) {
        if (receiver == null || receiver.isBlank()) {
            throw new IllegalArgumentException("接收人不能为空");
        }
        this.id = id;
        this.type = type;
        this.receiver = receiver;
        this.title = title;
        this.content = content;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
    }

    /** 标记发送成功。 */
    public void markSent() {
        this.status = "SENT";
        this.sentAt = LocalDateTime.now();
    }

    /** 标记发送失败（TODO：记录失败原因与重试次数）。 */
    public void markFailed() {
        this.status = "FAILED";
    }

    // ---------- getters ----------

    public String getId() {
        return id;
    }

    public NotificationType getType() {
        return type;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }
}
