-- ============================================================
-- tribunal-fulfillment-service 独立数据库（履约执行域）
-- 使用：mysql -uroot -p < fulfillment.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS tribunal_fulfillment
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE tribunal_fulfillment;

-- ------------------------------------------------------------
-- 1. 履约单主表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_fulfillment_order;
CREATE TABLE t_fulfillment_order (
    id              VARCHAR(64)   NOT NULL COMMENT '主键',
    source_order_no VARCHAR(64)   NOT NULL COMMENT '上游订单编号（业务唯一键）',
    customer_id     VARCHAR(64)   NOT NULL COMMENT '客户ID',
    status          VARCHAR(32)   NOT NULL COMMENT '状态（GENERATED/SHIPPED/SIGNED/CANCELLED）',
    total_amount    DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '总金额',
    created_at      DATETIME      NULL COMMENT '创建时间',
    shipped_at      DATETIME      NULL COMMENT '发货时间',
    signed_at       DATETIME      NULL COMMENT '签收时间',
    update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_source_order_no (source_order_no)
) ENGINE = InnoDB COMMENT = '履约单主表';

-- ------------------------------------------------------------
-- 2. 履约单明细表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_fulfillment_line;
CREATE TABLE t_fulfillment_line (
    id             VARCHAR(64)   NOT NULL COMMENT '主键',
    fulfillment_id VARCHAR(64)   NOT NULL COMMENT '履约单ID',
    sku_code       VARCHAR(64)   NOT NULL COMMENT 'SKU编码',
    sku_name       VARCHAR(128)  NULL COMMENT 'SKU名称',
    quantity       DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '数量',
    price          DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '单价',
    amount         DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '金额',
    create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (id),
    KEY idx_fulfillment (fulfillment_id)
) ENGINE = InnoDB COMMENT = '履约单明细表';

-- ------------------------------------------------------------
-- 3. 工厂指令表（发送工厂生产/备货指令）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_factory_order;
CREATE TABLE t_factory_order (
    id              VARCHAR(64)  NOT NULL COMMENT '主键',
    source_order_no VARCHAR(64)  NOT NULL COMMENT '上游订单编号',
    status          VARCHAR(32)  NOT NULL DEFAULT 'DISPATCHED' COMMENT '指令状态（DISPATCHED/ACKED）',
    dispatch_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下发时间',
    ack_time        DATETIME     NULL COMMENT '工厂回执时间',
    PRIMARY KEY (id),
    KEY idx_source_order (source_order_no)
) ENGINE = InnoDB COMMENT = '工厂指令表';
