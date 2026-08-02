-- ============================================================
-- tribunal-notification-service 独立数据库（通知域）
-- 使用：mysql -uroot -p < notification.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS tribunal_notification
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE tribunal_notification;

-- ------------------------------------------------------------
-- 1. 通知消息表（站内信/邮件/短信/微信）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_notification_message;
CREATE TABLE t_notification_message (
    id         VARCHAR(64)  NOT NULL COMMENT '主键',
    type       VARCHAR(32)  NOT NULL COMMENT '通知类型（SITE_MESSAGE/EMAIL/SMS/WECHAT）',
    receiver   VARCHAR(128) NOT NULL COMMENT '接收人（用户ID/邮箱/手机号）',
    title      VARCHAR(256) NULL COMMENT '标题',
    content    TEXT         NULL COMMENT '内容',
    status     VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '发送状态（PENDING/SENT/FAILED）',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    sent_at    DATETIME     NULL COMMENT '发送时间',
    PRIMARY KEY (id),
    KEY idx_receiver (receiver),
    KEY idx_status (status)
) ENGINE = InnoDB COMMENT = '通知消息表';

-- ------------------------------------------------------------
-- 2. 通知模板表（模板管理 F-704）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_notification_template;
CREATE TABLE t_notification_template (
    id                VARCHAR(64)  NOT NULL COMMENT '主键',
    template_code     VARCHAR(64)  NOT NULL COMMENT '模板编码',
    notification_type VARCHAR(32)  NOT NULL COMMENT '通知类型',
    title_template    VARCHAR(256) NULL COMMENT '标题模板（占位符）',
    content_template  TEXT         NULL COMMENT '内容模板（占位符）',
    active            TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用',
    PRIMARY KEY (id),
    UNIQUE KEY uk_template (template_code, notification_type)
) ENGINE = InnoDB COMMENT = '通知模板表';
