-- ============================================================
-- tribunal-auth-service 独立数据库（认证授权域）
-- 使用：mysql -uroot -p < auth.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS tribunal_auth
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE tribunal_auth;

-- ------------------------------------------------------------
-- 1. 用户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user (
    id        VARCHAR(64)  NOT NULL COMMENT '主键',
    username  VARCHAR(64)  NOT NULL COMMENT '用户名',
    password  VARCHAR(128) NOT NULL COMMENT '密码（BCrypt密文）',
    role_code VARCHAR(32)  NOT NULL DEFAULT 'DEALER' COMMENT '角色编码（RBAC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB COMMENT = '用户表';

-- ------------------------------------------------------------
-- 2. 角色表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_role;
CREATE TABLE t_role (
    id        VARCHAR(64) NOT NULL COMMENT '主键',
    role_code VARCHAR(32) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE = InnoDB COMMENT = '角色表';

INSERT INTO t_role (id, role_code, role_name) VALUES
('role-001', 'DEALER', '经销商'),
('role-002', 'SALES', '销售'),
('role-003', 'FINANCE', '财务'),
('role-004', 'ADMIN', '管理员');
