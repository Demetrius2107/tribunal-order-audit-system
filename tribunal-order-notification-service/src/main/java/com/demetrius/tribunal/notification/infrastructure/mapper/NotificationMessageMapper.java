package com.demetrius.tribunal.notification.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.notification.infrastructure.model.NotificationMessagePo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知消息 Mapper。
 */
@Mapper
public interface NotificationMessageMapper extends BaseMapper<NotificationMessagePo> {
}
