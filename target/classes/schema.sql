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