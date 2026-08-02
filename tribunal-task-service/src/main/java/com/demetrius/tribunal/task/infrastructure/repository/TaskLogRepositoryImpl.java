package com.demetrius.tribunal.task.infrastructure.repository;

import com.demetrius.tribunal.task.domain.model.TaskLog;
import com.demetrius.tribunal.task.domain.repository.TaskLogRepository;
import com.demetrius.tribunal.task.infrastructure.mapper.TaskLogMapper;
import com.demetrius.tribunal.task.infrastructure.model.TaskLogPo;
import org.springframework.stereotype.Repository;

/**
 * 任务日志仓储实现（MyBatis-Plus）。
 */
@Repository
public class TaskLogRepositoryImpl implements TaskLogRepository {

    private final TaskLogMapper taskLogMapper;

    public TaskLogRepositoryImpl(TaskLogMapper taskLogMapper) {
        this.taskLogMapper = taskLogMapper;
    }

    @Override
    public void save(TaskLog log) {
        TaskLogPo po = new TaskLogPo();
        po.setId(log.getId());
        po.setTaskCode(log.getTaskCode());
        po.setResult(log.getResult());
        po.setProcessedCount(log.getProcessedCount());
        po.setStartedAt(log.getStartedAt());
        po.setFinishedAt(log.getFinishedAt());
        taskLogMapper.insert(po);
    }
}
