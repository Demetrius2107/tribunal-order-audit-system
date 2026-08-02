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
-- 2. 促销规则表（客户型/客户组型）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_promotion_rule;
CREATE TABLE t_promotion_rule (
    id               VARCHAR(64)   NOT NULL COMMENT '主键',
    promotion_type   VARCHAR(32)   NOT NULL COMMENT '促销类型（CUSTOMER/CUSTOMER_GROUP）',
    promotion_target VARCHAR(64)   NOT NULL COMMENT '促销对象编码',
    discount_rate    DECIMAL(5,4)  NOT NULL DEFAULT 0 COMMENT '折扣率（0~1）',
    active           TINYINT       NOT NULL DEFAULT 1 COMMENT '是否启用',
    deleted          TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '促销规则表';

-- ------------------------------------------------------------
-- 3. 押金规则表（按包装类型）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_deposit_rule;
CREATE TABLE t_deposit_rule (
    id                 VARCHAR(64)   NOT NULL COMMENT '主键',
    sku_code           VARCHAR(64)   NOT NULL COMMENT 'SKU编码',
    deposit_type       VARCHAR(32)   NOT NULL COMMENT '押金类型',
    deposit_amount     DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '押金金额',
    included_in_price  TINYINT       NOT NULL DEFAULT 0 COMMENT '是否计入价格',
    deleted            TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '押金规则表';
