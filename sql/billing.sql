-- ============================================================
-- tribunal-billing-service 独立数据库（金融账单处理模块）
-- 使用：mysql -uroot -p < billing.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS tribunal_billing
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE tribunal_billing;

-- ------------------------------------------------------------
-- 1. 金融账单主表
--    source_order_no 唯一约束 = 转单幂等第一道防线（数据库层）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_bill;
CREATE TABLE t_bill (
    id              VARCHAR(64)   NOT NULL COMMENT '主键',
    source_order_no VARCHAR(64)   NOT NULL COMMENT '上游订单编号（业务唯一键）',
    customer_id     VARCHAR(64)   NOT NULL COMMENT '客户ID',
    status          VARCHAR(32)   NOT NULL COMMENT '状态（GENERATED/CONFIRMED/SETTLED/VERIFIED/CANCELLED）',
    total_amount    DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '账单总金额',
    generated_at    DATETIME      NULL COMMENT '生成时间',
    confirmed_at    DATETIME      NULL COMMENT '确认时间',
    settled_at      DATETIME      NULL COMMENT '结算/核销时间',
    update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_source_order_no (source_order_no),
    KEY idx_status (status)
) ENGINE = InnoDB COMMENT = '金融账单主表';

-- ------------------------------------------------------------
-- 2. 金融账单明细表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_bill_line;
CREATE TABLE t_bill_line (
    id           VARCHAR(64)   NOT NULL COMMENT '主键',
    bill_id      VARCHAR(64)   NOT NULL COMMENT '账单ID',
    sku_code     VARCHAR(64)   NOT NULL COMMENT 'SKU编码',
    sku_name     VARCHAR(128)  NULL COMMENT 'SKU名称',
    quantity     DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '数量',
    price        DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '单价',
    amount       DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '金额',
    create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted      TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (id),
    KEY idx_bill (bill_id)
) ENGINE = InnoDB COMMENT = '金融账单明细表';

-- ------------------------------------------------------------
-- 3. 收款流水表（结算时记录，审计 + 对账）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_bill_payment;
CREATE TABLE t_bill_payment (
    id           VARCHAR(64)   NOT NULL COMMENT '主键',
    bill_id      VARCHAR(64)   NOT NULL COMMENT '账单ID',
    source_order_no VARCHAR(64) NOT NULL COMMENT '上游订单编号',
    amount       DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '收款金额',
    payment_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收款时间',
    operator     VARCHAR(64)   NULL COMMENT '操作人',
    PRIMARY KEY (id),
    KEY idx_bill (bill_id)
) ENGINE = InnoDB COMMENT = '收款流水表';
