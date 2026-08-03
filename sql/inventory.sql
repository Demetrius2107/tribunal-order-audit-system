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
