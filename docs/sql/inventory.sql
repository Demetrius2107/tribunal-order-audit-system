-- ============================================================
-- tribunal-inventory-service 独立数据库（库存物料模块）
-- 使用：mysql -uroot -p < inventory.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS tribunal_inventory
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE tribunal_inventory;

-- ------------------------------------------------------------
-- 1. 库存物料表（物料主数据 + 库存账）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_inventory_item;
CREATE TABLE t_inventory_item (
    id                VARCHAR(64)   NOT NULL COMMENT '主键',
    sku_code          VARCHAR(64)   NOT NULL COMMENT 'SKU编码（唯一）',
    sku_name          VARCHAR(128)  NULL COMMENT 'SKU名称',
    unit              VARCHAR(32)   NULL COMMENT '单位',
    total_quantity    DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '总库存',
    reserved_quantity DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '已预占库存',
    version           INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（并发预占/释放防超卖）',
    deleted           TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sku_code (sku_code)
) ENGINE = InnoDB COMMENT = '库存物料表';

-- ------------------------------------------------------------
-- 2. 库存变动流水表（预占/释放/入库审计）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_inventory_flow;
CREATE TABLE t_inventory_flow (
    id          VARCHAR(64)   NOT NULL COMMENT '主键',
    sku_code    VARCHAR(64)   NOT NULL COMMENT 'SKU编码',
    change_type VARCHAR(32)   NOT NULL COMMENT '变动类型（IN/OUT/RESERVE/RELEASE）',
    quantity    DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '变动数量',
    source_no   VARCHAR(64)   NULL COMMENT '来源单号（订单号等）',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变动时间',
    PRIMARY KEY (id),
    KEY idx_sku (sku_code)
) ENGINE = InnoDB COMMENT = '库存变动流水表';

-- ------------------------------------------------------------
-- 初始化测试数据
-- ------------------------------------------------------------
INSERT INTO t_inventory_item (id, sku_code, sku_name, unit, total_quantity, reserved_quantity)
VALUES ('inv-001', 'SKU001', '测试商品A', '件', 1000.00, 0.00);

-- ------------------------------------------------------------
-- 3. M4 仓库主数据表（寻源分仓的基础数据）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_warehouse;
CREATE TABLE t_warehouse (
    id            VARCHAR(64)  NOT NULL COMMENT '主键（仓库ID）',
    warehouse_no  VARCHAR(64)  NOT NULL COMMENT '仓库编号（业务唯一键）',
    warehouse_name VARCHAR(128) NULL COMMENT '仓库名称',
    enabled       TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用 0否1是',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_warehouse_no (warehouse_no)
) ENGINE = InnoDB COMMENT = 'M4 仓库主数据表（寻源分仓基础数据）';

-- ------------------------------------------------------------
-- 4. M4 仓库级库存表（按 SKU 维度记录各仓库的可用库存，供寻源匹配）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_warehouse_stock;
CREATE TABLE t_warehouse_stock (
    id                 VARCHAR(64)   NOT NULL COMMENT '主键',
    warehouse_id       VARCHAR(64)   NOT NULL COMMENT '仓库ID（关联 t_warehouse.id）',
    sku_code           VARCHAR(64)   NOT NULL COMMENT 'SKU编码',
    available_quantity DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '可用库存（可售/可发货）',
    create_time        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted            TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wh_sku (warehouse_id, sku_code),
    KEY idx_sku (sku_code)
) ENGINE = InnoDB COMMENT = 'M4 仓库级库存表（寻源分仓依据）';

-- ------------------------------------------------------------
-- M4 初始化测试数据：两仓两 SKU（用于单仓/多仓/缺货寻源测试）
-- ------------------------------------------------------------
INSERT INTO t_warehouse (id, warehouse_no, warehouse_name) VALUES
    ('WH-A', 'WH001', '中心仓A'),
    ('WH-B', 'WH002', '区域仓B');

INSERT INTO t_warehouse_stock (id, warehouse_id, sku_code, available_quantity) VALUES
    ('ws-1', 'WH-A', 'SKU001', 500.00),
    ('ws-2', 'WH-B', 'SKU001', 300.00),
    ('ws-3', 'WH-A', 'SKU002', 200.00),
    ('ws-4', 'WH-B', 'SKU002', 100.00);
