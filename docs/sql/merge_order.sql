-- =============================================================================
-- M4: 合单（订单合并发货）建表脚本
--
-- 业务说明：
--   合单是拆单的对称操作——把 N 个同收货人订单合并成 1 个发货单，
--   减少物流成本。明细表保留来源订单可追溯性。
--
-- 表清单：
--   t_merge_order       - 合单主表
--   t_merge_order_item  - 合单明细（来源订单 + SKU）
-- =============================================================================

-- 合单主表
CREATE TABLE IF NOT EXISTS `t_merge_order` (
    `id`           VARCHAR(32)  NOT NULL COMMENT '合单ID',
    `merge_no`     VARCHAR(64)  NOT NULL COMMENT '合单编号（业务唯一键，如 MG20260808...）',
    `customer_id`  VARCHAR(64)  NOT NULL COMMENT '合单客户ID（所有成员订单共享）',
    `status`       VARCHAR(20)  NOT NULL DEFAULT 'CREATED' COMMENT '状态：CREATED/PACKED/SHIPPED/DELIVERED/CANCELLED',
    `shipping_fee` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '合单运费',
    `tracking_no`  VARCHAR(128) DEFAULT NULL COMMENT '物流单号（发货时填入）',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_merge_no` (`merge_no`),
    KEY `idx_customer_id` (`customer_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合单（订单合并发货）';

-- 合单明细表
CREATE TABLE IF NOT EXISTS `t_merge_order_item` (
    `id`              VARCHAR(32)  NOT NULL COMMENT '明细ID',
    `merge_order_id`  VARCHAR(32)  NOT NULL COMMENT '合单ID',
    `order_id`        VARCHAR(64)  NOT NULL COMMENT '来源订单ID',
    `order_no`        VARCHAR(64)  NOT NULL COMMENT '来源订单编号',
    `sku_code`        VARCHAR(64)  NOT NULL COMMENT 'SKU编码',
    `sku_name`        VARCHAR(255) NOT NULL COMMENT 'SKU名称',
    `quantity`        DECIMAL(12,2) NOT NULL COMMENT '数量',
    `unit_amount`     DECIMAL(12,2) NOT NULL COMMENT '单价',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_merge_order_id` (`merge_order_id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合单明细';
