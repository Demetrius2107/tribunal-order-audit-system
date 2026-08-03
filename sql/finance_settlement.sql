-- ============================================================
-- tribunal-finance-settlement-service 独立数据库（金融结算系统独立库）
-- 使用：mysql -uroot -p < finance_settlement.sql
-- 对应 PRD《金融结算模块需求规格说明书》5.1 核心表结构
-- ============================================================

CREATE DATABASE IF NOT EXISTS tribunal_finance_settlement
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE tribunal_finance_settlement;

-- ------------------------------------------------------------
-- 1. 结算单主表（资金结算凭证，uk_settlement 业务唯一键）
--    PRD 6.1 状态机：PENDING→CHARGING→CHARGED→SPLITTING→SPLIT→SETTLED/CLOSED
-- ------------------------------------------------------------
DROP TABLE IF EXISTS settlement_order;
CREATE TABLE settlement_order (
    id                    VARCHAR(64)   NOT NULL COMMENT '主键（雪花ID）',
    settlement_id         VARCHAR(64)   NOT NULL COMMENT '结算单号（业务唯一键）',
    order_id              VARCHAR(64)   NOT NULL COMMENT '关联订单号',
    user_id               VARCHAR(64)   NOT NULL COMMENT '用户ID',
    merchant_id           VARCHAR(64)   NOT NULL COMMENT '商家ID',
    status                VARCHAR(32)   NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/CHARGING/CHARGED/SPLITTING/SPLIT/REFUNDING/REFUNDED/SETTLED/CLOSED',
    total_amount          DECIMAL(18,4) NOT NULL COMMENT '订单总金额',
    discount_amount       DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '优惠金额',
    shipping_fee          DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '运费',
    tax_amount            DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '税费',
    platform_fee          DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '平台服务费',
    payment_fee           DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '支付手续费',
    net_amount            DECIMAL(18,4) NOT NULL COMMENT '实付金额 = total - discount + shipping + tax',
    payment_method        VARCHAR(32)   NOT NULL COMMENT '支付方式',
    payment_currency      VARCHAR(8)    NOT NULL DEFAULT 'CNY' COMMENT '支付币种',
    channel_transaction_id VARCHAR(128) NULL COMMENT '支付渠道流水号',
    create_time           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_settlement (settlement_id),
    KEY idx_order (order_id),
    KEY idx_merchant_time (merchant_id, create_time),
    KEY idx_status (status)
) ENGINE = InnoDB COMMENT = '结算单主表';

-- ------------------------------------------------------------
-- 2. 结算明细表（账单拆解明细项）
--    PRD 2.1.2 FR-004：GOODS/SHIPPING/DISCOUNT/TAX/PLATFORM_FEE/PAYMENT_FEE
-- ------------------------------------------------------------
DROP TABLE IF EXISTS settlement_detail;
CREATE TABLE settlement_detail (
    id             VARCHAR(64)   NOT NULL COMMENT '主键（雪花ID）',
    settlement_id  VARCHAR(64)   NOT NULL COMMENT '结算单号',
    item_type      VARCHAR(32)   NOT NULL COMMENT '明细项类型：GOODS/SHIPPING/DISCOUNT/TAX/PLATFORM_FEE/PAYMENT_FEE',
    sku_id         VARCHAR(64)   NULL COMMENT 'SKU编码（商品类）',
    sku_name       VARCHAR(256)  NULL COMMENT 'SKU名称',
    quantity       INT           NOT NULL DEFAULT 0 COMMENT '数量',
    unit_price     DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '单价',
    original_amount DECIMAL(18,4) NOT NULL COMMENT '原始金额',
    actual_amount  DECIMAL(18,4) NOT NULL COMMENT '实际金额（优惠分摊后）',
    description    VARCHAR(512)  NULL COMMENT '描述',
    create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_settlement (settlement_id)
) ENGINE = InnoDB COMMENT = '结算明细表';

-- ------------------------------------------------------------
-- 3. 分账记录表（按结算单拆分资金到各分账方）
--    PRD 2.3.1 FR-025：分账比例之和 = 100%
-- ------------------------------------------------------------
DROP TABLE IF EXISTS split_record;
CREATE TABLE split_record (
    id                    VARCHAR(64)   NOT NULL COMMENT '主键（雪花ID）',
    settlement_id         VARCHAR(64)   NOT NULL COMMENT '结算单号',
    recipient_id          VARCHAR(64)   NOT NULL COMMENT '收款方ID',
    recipient_type        VARCHAR(32)   NOT NULL COMMENT '收款方类型：MERCHANT/PLATFORM/LOGISTICS/AGENT',
    split_amount          DECIMAL(18,4) NOT NULL COMMENT '分账金额',
    split_rate            DECIMAL(5,4)  NOT NULL COMMENT '分账比例',
    status                VARCHAR(32)   NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/SUCCESS/FAILED',
    channel_transaction_id VARCHAR(128) NULL COMMENT '渠道流水号',
    create_time           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_settlement (settlement_id),
    KEY idx_recipient (recipient_id, status)
) ENGINE = InnoDB COMMENT = '分账记录表';

-- ------------------------------------------------------------
-- 4. 退款记录表（逆向流程资金凭证）
--    PRD 2.4.3 状态机：PENDING→APPROVED→PROCESSING→SUCCESS/FAILED
-- ------------------------------------------------------------
DROP TABLE IF EXISTS refund_record;
CREATE TABLE refund_record (
    id                    VARCHAR(64)   NOT NULL COMMENT '主键（雪花ID）',
    refund_id             VARCHAR(64)   NOT NULL COMMENT '退款单号（业务唯一键）',
    settlement_id         VARCHAR(64)   NOT NULL COMMENT '原结算单号',
    original_order_id     VARCHAR(64)   NOT NULL COMMENT '原订单号',
    refund_type           VARCHAR(16)   NOT NULL COMMENT '退款类型：FULL/PARTIAL',
    refund_amount         DECIMAL(18,4) NOT NULL COMMENT '退款金额',
    reason                VARCHAR(512)  NULL COMMENT '退款原因',
    reason_code           VARCHAR(32)   NULL COMMENT '原因码：USER_CANCEL/USER_RETURN/PRICE_DIFF/SYSTEM_ERROR',
    status                VARCHAR(32)   NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED/PROCESSING/SUCCESS/FAILED',
    approver_id           VARCHAR(64)   NULL COMMENT '审核人ID',
    channel_transaction_id VARCHAR(128) NULL COMMENT '渠道流水号',
    create_time           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_refund (refund_id),
    KEY idx_settlement (settlement_id),
    KEY idx_status (status)
) ENGINE = InnoDB COMMENT = '退款记录表';

-- ------------------------------------------------------------
-- 5. 扣款幂等控制表（幂等键为主键）
--    幂等键：settlementId_batchNo（PRD 2.2.2 FR-017，杜绝重复扣款）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS payment_idempotent;
CREATE TABLE payment_idempotent (
    idempotency_key VARCHAR(128) NOT NULL COMMENT '幂等键：settlementId_batchNo',
    settlement_id   VARCHAR(64)  NOT NULL COMMENT '结算单号',
    status          VARCHAR(16)  NOT NULL COMMENT '状态：SUCCESS/FAILED/PROCESSING',
    channel_response TEXT        NULL COMMENT '渠道原始响应',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    expire_at       DATETIME     NOT NULL COMMENT '过期时间',
    PRIMARY KEY (idempotency_key),
    KEY idx_settlement (settlement_id)
) ENGINE = InnoDB COMMENT = '扣款幂等控制表';

-- ------------------------------------------------------------
-- 6. 分账方账户余额表（独立虚拟账户，乐观锁防并发透支）
--    PRD 2.3.3 FR-032/FR-035
-- ------------------------------------------------------------
DROP TABLE IF EXISTS account_balance;
CREATE TABLE account_balance (
    id               VARCHAR(64)   NOT NULL COMMENT '主键（雪花ID）',
    account_id       VARCHAR(64)   NOT NULL COMMENT '账户ID（业务唯一键）',
    owner_id         VARCHAR(64)   NOT NULL COMMENT '所属方ID（商户/物流/平台）',
    owner_type       VARCHAR(32)   NOT NULL COMMENT '所属方类型',
    available_balance DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '可用余额',
    frozen_balance   DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '冻结余额',
    in_transit_amount DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '在途金额',
    currency         VARCHAR(8)    NOT NULL DEFAULT 'CNY' COMMENT '币种',
    version          BIGINT        NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_account (account_id),
    KEY idx_owner (owner_id, owner_type)
) ENGINE = InnoDB COMMENT = '分账方账户余额表';

-- ------------------------------------------------------------
-- 7. 账户流水表（资金审计，支持追溯）
--    PRD 2.7.2 FR-069：扣款/退款/分账/提现等敏感操作必须记录审计日志
-- ------------------------------------------------------------
DROP TABLE IF EXISTS account_transaction;
CREATE TABLE account_transaction (
    id                  VARCHAR(64)   NOT NULL COMMENT '主键（雪花ID）',
    transaction_id      VARCHAR(64)   NOT NULL COMMENT '流水号（业务唯一键）',
    account_id          VARCHAR(64)   NOT NULL COMMENT '账户ID',
    transaction_type    VARCHAR(32)   NOT NULL COMMENT '交易类型：SPLIT_IN/WITHDRAW_OUT/REFUND_OUT/FREEZE/UNFREEZE',
    amount              DECIMAL(18,4) NOT NULL COMMENT '金额',
    related_settlement_id VARCHAR(64) NULL COMMENT '关联结算单号',
    related_order_id    VARCHAR(64)   NULL COMMENT '关联订单号',
    balance_after       DECIMAL(18,4) NOT NULL COMMENT '变动后余额',
    description         VARCHAR(512)  NULL COMMENT '描述',
    create_time         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_transaction (transaction_id),
    KEY idx_account_time (account_id, create_time),
    KEY idx_settlement (related_settlement_id)
) ENGINE = InnoDB COMMENT = '账户流水表';
