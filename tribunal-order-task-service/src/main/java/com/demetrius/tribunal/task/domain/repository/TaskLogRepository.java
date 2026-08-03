package com.demetrius.tribunal.task.domain.repository;

import com.demetrius.tribunal.task.domain.model.TaskLog;

/**
 * 任务日志仓储接口。
 */
public interface TaskLogRepository {

    void save(TaskLog log);
}
