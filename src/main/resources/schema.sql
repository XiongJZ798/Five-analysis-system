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
INSERT INTO test_data (bler, create_time, mac_throughput, mcs, mimo_rank, prb_num, rsrp, sinr, test_time, update_time)
SELECT 1.5, NOW(), 100.0, 15, 2, 50, -85.0, 15.0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM test_data);

-- 创建分析任务主表
CREATE TABLE IF NOT EXISTS analysis_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL COMMENT '上传的文件名',
    task_type VARCHAR(20) NOT NULL DEFAULT 'RULE_AI' COMMENT '任务类型: RULE_ONLY / RULE_AI',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态: PENDING/RUNNING/SUCCESS/FAILED/PARTIAL_SUCCESS',
    progress INT NOT NULL DEFAULT 0 COMMENT '任务进度(0-100)',
    error_message TEXT NULL COMMENT '失败时的错误信息',
    created_by VARCHAR(100) NULL COMMENT '创建人',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI分析任务';

-- 创建规则分析结果表
CREATE TABLE IF NOT EXISTS analysis_result_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL COMMENT '关联的分析任务ID',
    kpi_summary_json MEDIUMTEXT NULL COMMENT 'KPI汇总JSON',
    timeseries_json MEDIUMTEXT NULL COMMENT '时序分析JSON',
    distribution_json MEDIUMTEXT NULL COMMENT '分布分析JSON',
    peak_rate_json MEDIUMTEXT NULL COMMENT '峰值速率分析JSON',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    INDEX idx_rule_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则分析结果';

-- 创建AI分析结果表
CREATE TABLE IF NOT EXISTS analysis_result_ai (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL COMMENT '关联的分析任务ID',
    model_name VARCHAR(100) NULL COMMENT '使用的AI模型名称',
    prompt_version VARCHAR(50) NULL COMMENT 'Prompt版本号',
    input_digest VARCHAR(64) NULL COMMENT '输入数据的摘要(SHA-256)',
    output_json MEDIUMTEXT NULL COMMENT 'AI结构化输出JSON',
    confidence_score DOUBLE NULL COMMENT '综合置信度(0-1)',
    token_usage INT NULL COMMENT '消耗的token数量',
    latency_ms BIGINT NULL COMMENT 'AI调用延迟(毫秒)',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    INDEX idx_ai_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI分析结果';

-- 创建分析反馈表
CREATE TABLE IF NOT EXISTS analysis_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL COMMENT '关联的分析任务ID',
    finding_id VARCHAR(50) NULL COMMENT '关联的发现项ID',
    feedback_type VARCHAR(20) NOT NULL COMMENT '反馈类型: ACCEPT/REJECT/PARTIAL',
    comment TEXT NULL COMMENT '反馈说明',
    created_by VARCHAR(100) NULL COMMENT '反馈人',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    INDEX idx_feedback_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI分析反馈'; 