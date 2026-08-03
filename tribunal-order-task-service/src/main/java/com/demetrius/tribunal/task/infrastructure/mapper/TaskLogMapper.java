package com.demetrius.tribunal.task.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.task.infrastructure.model.TaskLogPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务日志 Mapper。
 */
@Mapper
public interface TaskLogMapper extends BaseMapper<TaskLogPo> {
}
