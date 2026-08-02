package com.demetrius.tribunal.notification.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知消息持久化对象（对应 t_notification_message 表）。
 */
@Data
@TableName("t_notification_message")
public class NotificationMessagePo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 通知类型：SITE_MESSAGE/EMAIL/SMS/WECHAT */
    private String type;

    private String receiver;

    private String title;

    private String content;

    /** 发送状态：PENDING/SENT/FAILED */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime sentAt;
}
