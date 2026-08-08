-- ============================================================
-- tribunal-marketing-service 独立数据库（营销价格域）
-- 使用：mysql -uroot -p < marketing.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS tribunal_marketing
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE tribunal_marketing;

-- ------------------------------------------------------------
-- 1. 价格规则表（客户价/客户组价/区域价）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_price_rule;
CREATE TABLE t_price_rule (
    id           VARCHAR(64)   NOT NULL COMMENT '主键',
    sku_code     VARCHAR(64)   NOT NULL COMMENT 'SKU编码',
    price_level  VARCHAR(32)   NOT NULL COMMENT '价格档位（CUSTOMER/CUSTOMER_GROUP/AREA）',
    price_target VARCHAR(64)   NOT NULL COMMENT '价格对象编码（客户/客户组/区域）',
    price        DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '价格',
    currency     VARCHAR(16)   NOT NULL DEFAULT 'CNY' COMMENT '币种',
    deleted      TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sku_level_target (sku_code, price_level, price_target)
) ENGINE = InnoDB COMMENT = '价格规则表';

-- ------------------------------------------------------------
-- 2. 促销规则表（满减/折扣/第二件半价/满赠，支持叠加/互斥）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_promotion_rule;
CREATE TABLE t_promotion_rule (
    id                 VARCHAR(64)   NOT NULL COMMENT '主键',
    rule_no            VARCHAR(64)   NOT NULL COMMENT '规则编号',
    name               VARCHAR(128)  NOT NULL COMMENT '规则名称',
    type               VARCHAR(32)   NOT NULL COMMENT '促销类型（FULL_REDUCTION/DISCOUNT/SECOND_HALF_PRICE/GIFT）',
    target_type        VARCHAR(32)   NOT NULL DEFAULT 'ALL' COMMENT '适用对象（ALL/CUSTOMER/CUSTOMER_GROUP）',
    target_value       VARCHAR(64)   NULL COMMENT '适用对象编码（ALL时为空）',
    threshold          DECIMAL(18,2) NULL COMMENT '满减/满赠门槛金额',
    discount_rate      DECIMAL(5,4)  NULL COMMENT '折扣率（0~1，DISCOUNT专用）',
    reduction_amount   DECIMAL(18,2) NULL COMMENT '满减金额（FULL_REDUCTION专用）',
    half_price_rate    DECIMAL(5,4)  NULL DEFAULT 0.5000 COMMENT '第二件折扣率（SECOND_HALF_PRICE专用，默认半价）',
    applicable_sku_code VARCHAR(64)  NULL COMMENT '限定SKU编码（NULL=不限定，对整单生效）',
    gift_sku_code      VARCHAR(64)   NULL COMMENT '赠品SKU编码（GIFT专用）',
    gift_sku_name      VARCHAR(128)  NULL COMMENT '赠品名称',
    gift_quantity      DECIMAL(18,4) NULL COMMENT '赠品数量',
    exclusive          TINYINT       NOT NULL DEFAULT 0 COMMENT '是否互斥（1=应用后终止后续规则）',
    priority           INT           NOT NULL DEFAULT 0 COMMENT '优先级（升序应用）',
    active             TINYINT       NOT NULL DEFAULT 1 COMMENT '是否启用',
    start_time         DATETIME      NULL COMMENT '生效时间',
    end_time           DATETIME      NULL COMMENT '失效时间',
    deleted            TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (id),
    KEY idx_active (active),
    KEY idx_applicable_sku (applicable_sku_code)
) ENGINE = InnoDB COMMENT = '促销规则表（M4促销引擎）';

-- 测试数据：满100减20（可叠加）、全单九折（互斥）、SKU001第二件半价、满200赠啤酒杯
INSERT INTO t_promotion_rule (id, rule_no, name, type, target_type, target_value, threshold, discount_rate, reduction_amount, half_price_rate, applicable_sku_code, gift_sku_code, gift_sku_name, gift_quantity, exclusive, priority, active) VALUES
('PR001', 'P-001', '满100减20', 'FULL_REDUCTION', 'ALL', NULL, 100.00, NULL, 20.00, NULL, NULL, NULL, NULL, NULL, 0, 10, 1),
('PR002', 'P-002', '全单九折', 'DISCOUNT', 'ALL', NULL, NULL, 0.9000, NULL, NULL, NULL, NULL, NULL, NULL, 1, 20, 1),
('PR003', 'P-003', 'SKU001第二件半价', 'SECOND_HALF_PRICE', 'ALL', NULL, NULL, NULL, NULL, 0.5000, 'SKU001', NULL, NULL, NULL, 0, 30, 1),
('PR004', 'P-004', '满200赠啤酒杯', 'GIFT', 'ALL', NULL, 200.00, NULL, NULL, NULL, NULL, 'GIFT001', '定制啤酒杯', 2.0000, 0, 40, 1);

-- ------------------------------------------------------------
-- 3. 押金规则表（按包装类型，5类：瓶/箱/桶/托盘/坛）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_deposit_rule;
CREATE TABLE t_deposit_rule (
    id                 VARCHAR(64)   NOT NULL COMMENT '主键',
    sku_code           VARCHAR(64)   NOT NULL COMMENT 'SKU编码',
    packaging_type     VARCHAR(32)   NOT NULL COMMENT '包装类型（BOTTLE/BOX/KEG/TRAY/JAR）',
    unit_deposit       DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '单件押金',
    included_in_price  TINYINT       NOT NULL DEFAULT 0 COMMENT '押金是否已含在售价中（1=不再额外加收）',
    active             TINYINT       NOT NULL DEFAULT 1 COMMENT '是否启用',
    deleted            TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sku_packaging (sku_code, packaging_type)
) ENGINE = InnoDB COMMENT = '押金规则表（M4押金引擎）';

-- 测试数据：瓶装2元/件（额外加收）、箱装10元/件（额外加收）、托盘50元/件（额外加收）
INSERT INTO t_deposit_rule (id, sku_code, packaging_type, unit_deposit, included_in_price, active) VALUES
('DR001', 'SKU001', 'BOTTLE', 2.00, 0, 1),
('DR002', 'SKU002', 'BOX', 10.00, 0, 1),
('DR003', 'SKU003', 'TRAY', 50.00, 0, 1);
