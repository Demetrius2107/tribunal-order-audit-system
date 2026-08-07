package com.demetrius.tribunal.order.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.order.infrastructure.model.OutboxMessagePo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 本地消息表 Mapper（M3 异步化）。
 */
@Mapper
public interface OutboxMessageMapper extends BaseMapper<OutboxMessagePo> {

    /**
     * relay 专用：标记消息已投递（乐观锁防并发重复发送）。
     */
    @Update("UPDATE t_outbox_message SET status='SENT', sent_time=#{now}, version=version+1 " +
            "WHERE id=#{id} AND version=#{version}")
    int markSent(@Param("id") Long id, @Param("version") Integer version, @Param("now") LocalDateTime now);

    /**
     * relay 专用：递增重试次数 + 设置下次重试时间（指数退避）。
     */
    @Update("UPDATE t_outbox_message SET retry_count=retry_count+1, next_retry_time=#{nextRetry}, version=version+1 " +
            "WHERE id=#{id} AND version=#{version}")
    int incrementRetry(@Param("id") Long id, @Param("version") Integer version, @Param("nextRetry") LocalDateTime nextRetry);

    /**
     * relay 专用：超限后标记终态失败。
     */
    @Update("UPDATE t_outbox_message SET status='FAILED', version=version+1 WHERE id=#{id} AND version=#{version}")
    int markFailed(@Param("id") Long id, @Param("version") Integer version);
}
