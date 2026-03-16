-- 创建测试数据表
CREATE TABLE test_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- 测试时间，精确到毫秒
    test_time DATETIME(3) NOT NULL COMMENT '测试时间(精确到毫秒)',
    
    -- RSRP: 参考信号接收功率，范围-140dBm到-44dBm
    rsrp DOUBLE NULL COMMENT '参考信号接收功率(dBm)',
    
    -- SINR: 信噪比，范围-20dB到40dB
    sinr DOUBLE NULL COMMENT '信噪比(dB)',
    
    -- MAC层下行速率，单位Mbps
    mac_throughput DOUBLE NULL COMMENT 'MAC层下行速率(Mbps)',
    
    -- Rank: MIMO传输层数，范围1-8
    mimo_rank INT NULL COMMENT 'MIMO传输层数(1-8)',
    
    -- MCS: 调制编码方案，范围0-28
    mcs INT NULL COMMENT '调制编码方案(0-28)',
    
    -- PRB数: 物理资源块数，最大275
    prb_num INT NULL COMMENT '物理资源块数(最大275)',
    
    -- BLER: 块错误率，范围0-100%
    bler DOUBLE NULL COMMENT '块错误率(%)',
    
    -- 记录创建和更新时间
    create_time DATETIME NOT NULL COMMENT '记录创建时间',
    update_time DATETIME NOT NULL COMMENT '记录更新时间',
    
    -- 索引优化查询性能
    INDEX idx_test_time (test_time),
    INDEX idx_mac_throughput (mac_throughput)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='5G测试数据';

-- 创建时序数据表
CREATE TABLE time_series_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    time INT NOT NULL COMMENT '时间点',
    rate DOUBLE NOT NULL COMMENT '速率值',
    INDEX idx_file_name_time (file_name, time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='时序数据';

-- 初始化默认数据（可选）
INSERT INTO test_data (bler, create_time, mac_throughput, mcs, mimo_rank, rb_num, rsrp, sinr, test_time, update_time)
SELECT 1.5, NOW(), 100.0, 15, 2, 50, -85.0, 15.0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM test_data LIMIT 1);

-- ============================================================
-- AI 驱动分析流水线相关表（Rule + AI 双引擎）
-- ============================================================

-- 分析任务主表
CREATE TABLE IF NOT EXISTS analysis_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    upload_file_id      VARCHAR(255)    NOT NULL    COMMENT '关联上传文件标识',
    task_type           VARCHAR(20)     NOT NULL    COMMENT '任务类型: RULE_ONLY / RULE_AI',
    status              VARCHAR(30)     NOT NULL    DEFAULT 'PENDING' COMMENT '任务状态',
    progress            INT             NOT NULL    DEFAULT 0         COMMENT '进度 0-100',
    error_message       TEXT            NULL        COMMENT '失败原因',
    created_by          VARCHAR(255)    NULL        COMMENT '创建人',
    created_at          DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_at_file_id (upload_file_id),
    INDEX idx_at_status  (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分析任务';

-- 规则分析结果表
CREATE TABLE IF NOT EXISTS analysis_result_rule (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id             BIGINT          NOT NULL    COMMENT '关联任务ID',
    kpi_summary_json    LONGTEXT        NULL        COMMENT 'KPI汇总(JSON)',
    timeseries_json     LONGTEXT        NULL        COMMENT '时序分析结果(JSON)',
    distribution_json   LONGTEXT        NULL        COMMENT '速率分布(JSON)',
    peak_rate_json      LONGTEXT        NULL        COMMENT '峰值速率计算结果(JSON)',
    created_at          DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_arr_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则分析结果';

-- AI 分析结果表
CREATE TABLE IF NOT EXISTS analysis_result_ai (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id             BIGINT          NOT NULL    COMMENT '关联任务ID',
    model_name          VARCHAR(100)    NOT NULL    COMMENT '模型名称',
    prompt_version      VARCHAR(50)     NOT NULL    COMMENT '提示词版本',
    input_digest        VARCHAR(64)     NULL        COMMENT '输入摘要(SHA-256前缀)',
    output_json         LONGTEXT        NULL        COMMENT 'AI结构化输出(JSON)',
    confidence_score    DOUBLE          NULL        COMMENT '置信度得分',
    token_usage         INT             NULL        COMMENT 'Token用量',
    latency_ms          BIGINT          NULL        COMMENT '调用耗时(ms)',
    created_at          DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ara_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI分析结果';

-- 工程师反馈表（闭环学习）
CREATE TABLE IF NOT EXISTS analysis_feedback (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id             BIGINT          NOT NULL    COMMENT '关联任务ID',
    finding_id          VARCHAR(50)     NULL        COMMENT '关联发现ID (如 F1)',
    feedback_type       VARCHAR(20)     NOT NULL    COMMENT 'ACCEPT / REJECT / PARTIAL',
    comment             TEXT            NULL        COMMENT '备注',
    created_by          VARCHAR(255)    NULL        COMMENT '反馈人',
    created_at          DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_af_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分析反馈'; 