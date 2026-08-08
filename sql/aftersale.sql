-- =====================================================
-- 售后退货模块建表脚本
-- =====================================================

-- 售后单主表
CREATE TABLE IF NOT EXISTS t_after_sale (
    id                  VARCHAR(32)     NOT NULL COMMENT '主键ID',
    after_sale_no       VARCHAR(32)     NOT NULL COMMENT '售后单号（业务唯一键）',
    order_id            VARCHAR(32)     NOT NULL COMMENT '原订单ID',
    order_no            VARCHAR(32)     NOT NULL COMMENT '原订单编号',
    customer_id         VARCHAR(32)     NOT NULL COMMENT '客户ID',
    type                VARCHAR(20)     NOT NULL COMMENT '售后类型 RETURN_REFUND/REFUND_ONLY',
    reason              VARCHAR(30)     NOT NULL COMMENT '售后原因 QUALITY_ISSUE/DAMAGED/...',
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/APPROVED/COMPLETED/REJECTED',
    total_refund_amount DECIMAL(12, 2)  NOT NULL DEFAULT 0 COMMENT '退款总额（商品退款+押金退还）',
    reject_reason       VARCHAR(255)    NULL COMMENT '拒绝原因',
    refund_txn_no       VARCHAR(64)     NULL COMMENT '退款流水号',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_after_sale_no (after_sale_no),
    KEY idx_order_id (order_id),
    KEY idx_customer_id (customer_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后单';

-- 售后明细表
CREATE TABLE IF NOT EXISTS t_after_sale_item (
    id              VARCHAR(32)     NOT NULL COMMENT '主键ID',
    after_sale_id   VARCHAR(32)     NOT NULL COMMENT '售后单ID',
    sku_code        VARCHAR(32)     NOT NULL COMMENT 'SKU编码',
    sku_name        VARCHAR(128)    NULL COMMENT 'SKU名称',
    quantity        DECIMAL(12, 2)  NOT NULL COMMENT '退货数量',
    refund_amount   DECIMAL(12, 2)  NOT NULL DEFAULT 0 COMMENT '商品退款金额',
    deposit_refund  DECIMAL(12, 2)  NOT NULL DEFAULT 0 COMMENT '押金退还金额',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_after_sale_id (after_sale_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后明细';
