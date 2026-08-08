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

-- ------------------------------------------------------------
-- 3. 权限点表（RBAC：接口级权限点，如 order:review 审单）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_permission;
CREATE TABLE t_permission (
    id              VARCHAR(64) NOT NULL COMMENT '主键',
    permission_code VARCHAR(64) NOT NULL COMMENT '权限码（如 order:review）',
    permission_name VARCHAR(64) NOT NULL COMMENT '权限名称',
    module          VARCHAR(32) NOT NULL DEFAULT 'ORDER' COMMENT '所属模块（ORDER/CUSTOMER/INVENTORY/BILLING/MARKETING/FULFILLMENT/NOTIFICATION/FINANCE/AUTH）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_permission_code (permission_code)
) ENGINE = InnoDB COMMENT = '权限点表';

INSERT INTO t_permission (id, permission_code, permission_name, module) VALUES
-- 订单域
('perm-001', 'order:create',     '下单',        'ORDER'),
('perm-002', 'order:cancel',     '取消订单',    'ORDER'),
('perm-003', 'order:modify',     '改单',        'ORDER'),
('perm-004', 'order:review',     '审单',        'ORDER'),
('perm-005', 'order:view',       '查看订单',    'ORDER'),
-- 客户信用域
('perm-006', 'customer:credit',  '信用查询/占用', 'CUSTOMER'),
-- 库存域
('perm-007', 'inventory:manage', '库存管理',    'INVENTORY'),
('perm-008', 'inventory:reserve','库存预占/释放','INVENTORY'),
-- 财务域
('perm-009', 'billing:settle',   '账单结算/核销', 'BILLING'),
('perm-010', 'finance:settle',   '金融结算',    'FINANCE'),
-- 营销域
('perm-011', 'marketing:price',  '价格/促销配置', 'MARKETING'),
-- 履约域
('perm-012', 'fulfillment:ship', '发货/签收',   'FULFILLMENT'),
-- 通知域
('perm-013', 'notification:send','发送通知',    'NOTIFICATION'),
-- 系统域
('perm-014', 'auth:user',        '用户管理',    'AUTH'),
('perm-015', 'auth:permission',  '权限管理',    'AUTH');

-- ------------------------------------------------------------
-- 4. 角色-权限关联表（RBAC N:M）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_role_permission;
CREATE TABLE t_role_permission (
    id              VARCHAR(64) NOT NULL COMMENT '主键',
    role_code       VARCHAR(32) NOT NULL COMMENT '角色编码',
    permission_code VARCHAR(64) NOT NULL COMMENT '权限码',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_perm (role_code, permission_code)
) ENGINE = InnoDB COMMENT = '角色-权限关联表';

-- 经销商（DEALER）：只能下单/取消/查看自己的订单
INSERT INTO t_role_permission (id, role_code, permission_code) VALUES
('rp-001', 'DEALER', 'order:create'),
('rp-002', 'DEALER', 'order:cancel'),
('rp-003', 'DEALER', 'order:view');

-- 销售（SALES）：下单/取消/查看/改单/审单 + 客户信用
INSERT INTO t_role_permission (id, role_code, permission_code) VALUES
('rp-101', 'SALES', 'order:create'),
('rp-102', 'SALES', 'order:cancel'),
('rp-103', 'SALES', 'order:view'),
('rp-104', 'SALES', 'order:modify'),
('rp-105', 'SALES', 'order:review'),
('rp-106', 'SALES', 'customer:credit');

-- 财务（FINANCE）：查看订单 + 账单结算核销 + 金融结算
INSERT INTO t_role_permission (id, role_code, permission_code) VALUES
('rp-201', 'FINANCE', 'order:view'),
('rp-202', 'FINANCE', 'billing:settle'),
('rp-203', 'FINANCE', 'finance:settle');

-- 管理员（ADMIN）：全部权限
INSERT INTO t_role_permission (id, role_code, permission_code)
SELECT CONCAT('rp-admin-', t_permission.id), 'ADMIN', t_permission.permission_code
FROM t_permission;
