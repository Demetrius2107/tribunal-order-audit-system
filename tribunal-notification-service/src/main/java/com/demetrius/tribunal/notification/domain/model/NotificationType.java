package com.demetrius.tribunal.notification.domain.model;

/**
 * 通知类型枚举。
 *
 * <p>对应需求：F-701~F-704（站内信/邮件/短信/模板管理）。</p>
 */
public enum NotificationType {

    /** 站内信 */
    SITE_MESSAGE("站内信"),

    /** 邮件 */
    EMAIL("邮件"),

    /** 短信 */
    SMS("短信"),

    /** 微信模板消息 */
    WECHAT("微信");

    private final String desc;

    NotificationType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
