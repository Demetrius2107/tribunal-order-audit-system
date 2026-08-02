-- ============================================================
-- tribunal-customer-service 独立数据库（微服务独立库）
-- 使用：mysql -uroot -p < customer.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS tribunal_customer
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE tribunal_customer;

-- ------------------------------------------------------------
-- 客户表（客户/信用领域）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_customer;
CREATE TABLE t_customer (
    id            VARCHAR(64)   NOT NULL COMMENT '主键',
    customer_code VARCHAR(64)   NOT NULL COMMENT '客户编码',
    name          VARCHAR(128)  NOT NULL COMMENT '客户名称',
    credit_limit  DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '信用总额度',
    credit_used   DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '已占用信用',
    deleted       TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_code (customer_code)
) ENGINE = InnoDB COMMENT = '客户表';

-- ------------------------------------------------------------
-- 初始化测试数据
-- ------------------------------------------------------------
INSERT INTO t_customer (id, customer_code, name, credit_limit, credit_used)
VALUES ('cust-001', 'C001', '测试经销商A', 100000.00, 0.00);
