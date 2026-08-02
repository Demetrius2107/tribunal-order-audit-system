-- ============================================================
-- tribunal-erp-service 独立数据库（下游 ERP 履约系统）
-- 使用：mysql -uroot -p < erp.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS tribunal_erp
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE tribunal_erp;

-- ------------------------------------------------------------
-- 1. ERP 履约订单主表
--    source_order_no 唯一约束 = 转单幂等第一道防线（数据库层）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_erp_order;
CREATE TABLE t_erp_order (
    id              VARCHAR(64)   NOT NULL COMMENT '主键',
    source_order_no VARCHAR(64)   NOT NULL COMMENT '上游OMS订单编号（业务唯一键）',
    customer_id     VARCHAR(64)   NOT NULL COMMENT '客户ID',
    status          VARCHAR(32)   NOT NULL COMMENT '状态（RECEIVED/SHIPPED/SIGNED/CLOSED/CANCELLED）',
    total_amount    DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '总金额',
    received_at     DATETIME      NULL COMMENT '接收时间',
    shipped_at      DATETIME      NULL COMMENT '发货时间',
    signed_at       DATETIME      NULL COMMENT '签收时间',
    update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_source_order_no (source_order_no),
    KEY idx_status (status)
) ENGINE = InnoDB COMMENT = 'ERP履约订单主表';

-- ------------------------------------------------------------
-- 2. ERP 履约订单明细表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_erp_order_line;
CREATE TABLE t_erp_order_line (
    id           VARCHAR(64)   NOT NULL COMMENT '主键',
    erp_order_id VARCHAR(64)   NOT NULL COMMENT '履约订单ID',
    sku_code     VARCHAR(64)   NOT NULL COMMENT 'SKU编码',
    sku_name     VARCHAR(128)  NULL COMMENT 'SKU名称',
    quantity     DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '数量',
    price        DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '单价',
    amount       DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '金额',
    create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted      TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (id),
    KEY idx_erp_order (erp_order_id)
) ENGINE = InnoDB COMMENT = 'ERP履约订单明细表';
