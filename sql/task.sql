-- ============================================================
-- tribunal-task-service 独立数据库（定时任务域）
-- 使用：mysql -uroot -p < task.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS tribunal_task
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE tribunal_task;

-- ------------------------------------------------------------
-- 1. 任务执行日志表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_task_log;
CREATE TABLE t_task_log (
    id              VARCHAR(64) NOT NULL COMMENT '主键',
    task_code       VARCHAR(64) NOT NULL COMMENT '任务编码（TIMEOUT_CLOSE_ORDER/STATUS_RECONCILE/DATA_ARCHIVE）',
    result          VARCHAR(16) NOT NULL DEFAULT 'SUCCESS' COMMENT '执行结果（SUCCESS/FAILED）',
    processed_count INT         NOT NULL DEFAULT 0 COMMENT '处理条数',
    started_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    finished_at     DATETIME    NULL COMMENT '结束时间',
    PRIMARY KEY (id),
    KEY idx_task_code (task_code)
) ENGINE = InnoDB COMMENT = '任务执行日志表';
