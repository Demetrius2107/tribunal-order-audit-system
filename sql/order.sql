-- ============================================================
-- tribunal-order-service 独立数据库（微服务独立库）
-- 使用：mysql -uroot -p < order.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS tribunal_order
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE tribunal_order;

-- ------------------------------------------------------------
-- 1. 订单主表
--    order_no 唯一约束 = 幂等第一道防线（数据库层）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_order;
CREATE TABLE t_order (
    id              VARCHAR(64)   NOT NULL COMMENT '主键',
    order_no        VARCHAR(64)   NOT NULL COMMENT '订单编号（业务唯一键）',
    customer_id     VARCHAR(64)   NOT NULL COMMENT '客户ID（跨服务引用 customer-service）',
    status          VARCHAR(32)   NOT NULL COMMENT '状态（TO_BE_CONFIRMED等）',
    total_amount    DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '总金额',
    discount_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '折扣金额',
    payable_amount  DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '应付金额',
    reject_reason   VARCHAR(512)  NULL COMMENT '拒绝原因',
    create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_customer (customer_id),
    KEY idx_status (status)
) ENGINE = InnoDB COMMENT = '订单主表';

-- ------------------------------------------------------------
-- 2. 订单明细表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_order_sku;
CREATE TABLE t_order_sku (
    id          VARCHAR(64)   NOT NULL COMMENT '主键',
    order_id    VARCHAR(64)   NOT NULL COMMENT '订单ID',
    sku_code    VARCHAR(64)   NOT NULL COMMENT 'SKU编码',
    sku_name    VARCHAR(128)  NULL COMMENT 'SKU名称',
    quantity    DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '数量',
    price       DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '单价',
    amount      DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '金额',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (id),
    KEY idx_order (order_id)
) ENGINE = InnoDB COMMENT = '订单明细表';

-- ------------------------------------------------------------
-- 3. 订单状态流水表（审计 + 幂等判断依据）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_order_status_record;
CREATE TABLE t_order_status_record (
    id          VARCHAR(64)  NOT NULL COMMENT '主键',
    order_id    VARCHAR(64)  NOT NULL COMMENT '订单ID',
    from_status VARCHAR(32)  NULL COMMENT '原状态',
    to_status   VARCHAR(32)  NOT NULL COMMENT '目标状态',
    operator    VARCHAR(64)  NULL COMMENT '操作人',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_order (order_id)
) ENGINE = InnoDB COMMENT = '订单状态流水表';
