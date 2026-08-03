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
    order_type      VARCHAR(32)   NOT NULL DEFAULT 'NORMAL' COMMENT '订单类型：NORMAL普通/PRE_ORDER预购',
    car_pooling     TINYINT       NOT NULL DEFAULT 0 COMMENT '是否拼车订单 0否1是',
    car_pool_joined TINYINT       NOT NULL DEFAULT 0 COMMENT '是否已参与拼车 0否1是（已拼车不可关闭）',
    status          VARCHAR(32)   NOT NULL COMMENT '状态（TO_BE_CONFIRMED等）',
    total_amount    DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '总金额',
    discount_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '折扣金额',
    discount_pool_deduction DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '折扣池抵扣（用折扣池余额冲抵应付）',
    deposit_amount  DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '押金（包装物押金）',
    tax_amount      DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '税费',
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

-- ------------------------------------------------------------
-- 4. 空包装回收明细表（业务文档九节：经销商退回空包装物）
--    回收明细参与订单押金计算（回收押金合计计入应付金额）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_order_returnable;
CREATE TABLE t_order_returnable (
    id             VARCHAR(64)   NOT NULL COMMENT '主键',
    order_id       VARCHAR(64)   NOT NULL COMMENT '订单ID',
    packaging_type VARCHAR(64)   NOT NULL COMMENT '包装类型编码',
    packaging_name VARCHAR(128)  NULL COMMENT '包装类型名称',
    quantity       DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '回收数量',
    unit_deposit   DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '单个包装押金',
    deposit_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '押金合计（数量×单价押金）',
    deleted        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (id),
    KEY idx_order (order_id)
) ENGINE = InnoDB COMMENT = '空包装回收明细表';
