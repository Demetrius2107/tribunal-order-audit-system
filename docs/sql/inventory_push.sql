-- ============================================================
-- tribunal-inventory-push-service 独立数据库（库存推送系统独立库）
-- 使用：mysql -uroot -p < inventory_push.sql
-- 对应 PRD《库存推送模块需求规格说明书》5.1 核心表结构
-- ============================================================

CREATE DATABASE IF NOT EXISTS tribunal_inventory_push
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE tribunal_inventory_push;

-- ------------------------------------------------------------
-- 1. 库存主表（多维度库存，uk_sku_warehouse_owner 唯一键）
--    PRD 2.3.1 多维度库存管理：总/可用/锁定/在途/预留
-- ------------------------------------------------------------
DROP TABLE IF EXISTS inventory_sku;
CREATE TABLE inventory_sku (
    id             VARCHAR(64)  NOT NULL COMMENT '主键（雪花ID）',
    sku_id         VARCHAR(64)  NOT NULL COMMENT '标准化SKU编码',
    warehouse_id   VARCHAR(64)  NOT NULL COMMENT '仓库编码',
    owner_id       VARCHAR(64)  NOT NULL COMMENT '货主编码',
    total_qty      INT          NOT NULL DEFAULT 0 COMMENT '总库存（物理在库总量）',
    available_qty  INT          NOT NULL DEFAULT 0 COMMENT '可用库存（总-锁定-预留）',
    locked_qty     INT          NOT NULL DEFAULT 0 COMMENT '锁定库存（已审单未发货占用）',
    in_transit_qty INT          NOT NULL DEFAULT 0 COMMENT '在途库存',
    reserved_qty   INT          NOT NULL DEFAULT 0 COMMENT '预留库存',
    version        BIGINT       NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sku_warehouse_owner (sku_id, warehouse_id, owner_id)
) ENGINE = InnoDB COMMENT = '库存主表';

-- ------------------------------------------------------------
-- 2. 库存流水表（每次变动前后值 + 变动类型，支撑追溯）
--    PRD 2.3.1 FR-025：各维度库存变动必须记录流水明细
-- ------------------------------------------------------------
DROP TABLE IF EXISTS inventory_log;
CREATE TABLE inventory_log (
    id              VARCHAR(64)  NOT NULL COMMENT '主键（雪花ID）',
    sku_id          VARCHAR(64)  NOT NULL COMMENT 'SKU编码',
    warehouse_id    VARCHAR(64)  NOT NULL COMMENT '仓库编码',
    owner_id        VARCHAR(64)  NOT NULL COMMENT '货主编码',
    change_type     VARCHAR(32)  NOT NULL COMMENT '变动类型：PUSH/LOCK/UNLOCK/RESERVE',
    delta_qty       INT          NOT NULL COMMENT '变动数量（可正可负）',
    before_qty      INT          NOT NULL COMMENT '变动前数量',
    after_qty       INT          NOT NULL COMMENT '变动后数量',
    batch_id        VARCHAR(128) NULL COMMENT '关联批次号',
    source_batch_id VARCHAR(128) NULL COMMENT '上游推送批次号',
    message_id      VARCHAR(64)  NULL COMMENT '下游分发消息ID',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_sku_time (sku_id, create_time),
    KEY idx_batch (source_batch_id)
) ENGINE = InnoDB COMMENT = '库存流水表';

-- ------------------------------------------------------------
-- 3. 批次库存表（生产日期/有效期，支撑 FIFO/FEFO 与临期预警）
--    PRD 2.3.3 效期与批次管理
-- ------------------------------------------------------------
DROP TABLE IF EXISTS inventory_batch;
CREATE TABLE inventory_batch (
    id              VARCHAR(64)  NOT NULL COMMENT '主键（雪花ID）',
    sku_id          VARCHAR(64)  NOT NULL COMMENT 'SKU编码',
    warehouse_id    VARCHAR(64)  NOT NULL COMMENT '仓库编码',
    batch_no        VARCHAR(64)  NOT NULL COMMENT '批次号',
    production_date DATE         NULL COMMENT '生产日期',
    expiry_date     DATE         NULL COMMENT '有效期至',
    qty             INT          NOT NULL DEFAULT 0 COMMENT '批次数量',
    status          VARCHAR(16)  NOT NULL DEFAULT 'VALID' COMMENT '状态：VALID/EXPIRED/FROZEN',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sku_warehouse_batch (sku_id, warehouse_id, batch_no)
) ENGINE = InnoDB COMMENT = '批次库存表';

-- ------------------------------------------------------------
-- 4. 幂等控制表（幂等键为主键，有效期 7 天）
--    幂等键：batchId_skuId_warehouseId_version（PRD 2.5.1 FR-045）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS idempotent_record;
CREATE TABLE idempotent_record (
    idempotency_key VARCHAR(128) NOT NULL COMMENT '幂等键：batchId_skuId_warehouseId_version',
    batch_id        VARCHAR(128) NOT NULL COMMENT '上游推送批次号',
    status          VARCHAR(16)  NOT NULL DEFAULT 'SUCCESS' COMMENT '状态：SUCCESS/FAILED',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    expire_at       DATETIME     NOT NULL COMMENT '过期时间',
    PRIMARY KEY (idempotency_key),
    KEY idx_expire (expire_at)
) ENGINE = InnoDB COMMENT = '幂等控制表';
