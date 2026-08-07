package com.demetrius.tribunal.order.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 本地消息表 PO（Transactional Outbox Pattern）。
 *
 * <p>业务事务内 INSERT 一条 PENDING 记录 → relay 定时轮询投递 Kafka → 标记 SENT。
 * 保证"业务操作"与"消息发布"的原子性（M3 异步化）。</p>
 */
@Data
@TableName("t_outbox_message")
public class OutboxMessagePo {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 消息唯一 ID（幂等键，消费端据此去重） */
    private String messageId;

    /** Kafka topic */
    private String topic;

    /** Kafka partition key（orderId，保证同一订单的事件顺序消费） */
    private String messageKey;

    /** JSON 载荷 */
    private String payload;

    /** 状态：PENDING / SENT / FAILED */
    private String status;

    /** 已重试次数 */
    private Integer retryCount;

    private LocalDateTime createTime;

    /** 投递成功时间 */
    private LocalDateTime sentTime;

    /** 下次重试时间（指数退避） */
    private LocalDateTime nextRetryTime;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}
