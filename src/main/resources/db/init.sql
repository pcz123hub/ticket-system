CREATE DATABASE IF NOT EXISTS ticket_db DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ticket_db;

-- 工单主表
CREATE TABLE ticket (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_no     VARCHAR(32)  NOT NULL COMMENT '工单号',
    title         VARCHAR(200) NOT NULL COMMENT '标题',
    description   TEXT COMMENT '描述',
    category      VARCHAR(50)  NOT NULL COMMENT '业务类型',
    priority      TINYINT      NOT NULL DEFAULT 2 COMMENT '优先级 0=P0 1=P1 2=P2',
    status        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0=PENDING 1=PROCESSING 2=RESOLVED 3=CLOSED 4=TRANSFERRED',
    customer_id   BIGINT       NOT NULL COMMENT '客户ID',
    assignee_id   BIGINT       DEFAULT NULL COMMENT '当前处理人ID',
    source        VARCHAR(50)  NOT NULL COMMENT '来源',
    satisfaction  TINYINT      DEFAULT NULL COMMENT '满意度评分 1-5',
    version       INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    closed_at     DATETIME     DEFAULT NULL COMMENT '关闭时间',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ticket_no (ticket_no),
    KEY idx_status (status),
    KEY idx_assignee (assignee_id),
    KEY idx_customer (customer_id),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单表';

-- 操作日志表
CREATE TABLE ticket_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id     BIGINT       NOT NULL COMMENT '工单ID',
    from_status   TINYINT      DEFAULT NULL COMMENT '原状态',
    to_status     TINYINT      DEFAULT NULL COMMENT '目标状态',
    operator_id   BIGINT       NOT NULL COMMENT '操作人ID',
    remark        VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_ticket_id (ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单操作日志';

-- SLA 规则配置表
CREATE TABLE ticket_sla_config (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    category              VARCHAR(50) NOT NULL COMMENT '业务类型',
    priority              TINYINT     NOT NULL COMMENT '优先级',
    warn_timeout_minutes  INT         NOT NULL COMMENT '预警阈值(分钟)',
    escalate_timeout_minutes INT      NOT NULL COMMENT '升级阈值(分钟)',
    escalate_level        TINYINT     NOT NULL DEFAULT 1 COMMENT '升级级别 1=主管 2=经理',
    UNIQUE KEY uk_category_priority (category, priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SLA规则配置';

-- 分配记录表
CREATE TABLE ticket_assignment_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id     BIGINT   NOT NULL COMMENT '工单ID',
    from_agent_id BIGINT   DEFAULT NULL COMMENT '原处理人',
    to_agent_id   BIGINT   NOT NULL COMMENT '新处理人',
    strategy      VARCHAR(50) DEFAULT NULL COMMENT '分配策略',
    reason        VARCHAR(200) DEFAULT NULL COMMENT '原因',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_ticket_id (ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分配记录';

-- 升级记录表
CREATE TABLE ticket_sla_escalation (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id        BIGINT      NOT NULL COMMENT '工单ID',
    escalation_level TINYINT     NOT NULL COMMENT '升级级别 1=L1 2=L2',
    notified_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '通知时间',
    resolved_at      DATETIME    DEFAULT NULL COMMENT '解决时间',
    KEY idx_ticket_id (ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='升级记录';

-- 站内通知表
CREATE TABLE notification (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT       NOT NULL COMMENT '用户ID',
    type             VARCHAR(50)  NOT NULL COMMENT '类型',
    title            VARCHAR(200) NOT NULL COMMENT '标题',
    content          VARCHAR(1000) DEFAULT NULL COMMENT '内容',
    related_ticket_id BIGINT      DEFAULT NULL COMMENT '关联工单ID',
    is_read          TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_id (user_id),
    KEY idx_is_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知';

-- 客服人员表
CREATE TABLE cs_agent (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    name      VARCHAR(50)  NOT NULL COMMENT '姓名',
    skills    VARCHAR(200) DEFAULT NULL COMMENT '技能标签',
    status    TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0=离线 1=在线',
    max_load  INT          NOT NULL DEFAULT 10 COMMENT '最大并行处理数',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服人员';

-- 工单统计表（日维度归档）
CREATE TABLE ticket_stats (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    stats_date    DATE   NOT NULL COMMENT '统计日期',
    category      VARCHAR(50) DEFAULT NULL COMMENT '业务类型',
    total_count   INT    NOT NULL DEFAULT 0 COMMENT '工单总数',
    pending_count INT    NOT NULL DEFAULT 0,
    processing_count INT NOT NULL DEFAULT 0,
    resolved_count  INT NOT NULL DEFAULT 0,
    closed_count  INT    NOT NULL DEFAULT 0,
    avg_process_minutes INT DEFAULT 0 COMMENT '平均处理时长(分钟)',
    sla_pass_rate DECIMAL(5,2) DEFAULT 0 COMMENT 'SLA达标率',
    satisfaction_avg DECIMAL(3,2) DEFAULT NULL COMMENT '平均满意度',
    UNIQUE KEY uk_date_category (stats_date, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单日统计';

-- 插入默认 SLA 配置
INSERT INTO ticket_sla_config (category, priority, warn_timeout_minutes, escalate_timeout_minutes, escalate_level) VALUES
('GENERAL', 0, 30, 60, 2),
('GENERAL', 1, 240, 480, 1),
('GENERAL', 2, 1440, 2880, 1);
