-- 创建测试数据表
CREATE TABLE IF NOT EXISTS test_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bler DOUBLE NOT NULL COMMENT '块错误率(%)',
    create_time DATETIME NOT NULL COMMENT '记录创建时间',
    mac_throughput DOUBLE NOT NULL COMMENT 'MAC层下行速率(Mbps)',
    mcs INT NOT NULL COMMENT '调制编码方案',
    mimo_rank INT NOT NULL COMMENT 'MIMO传输层数',
    rb_num INT NOT NULL COMMENT '资源块数量',
    rsrp DOUBLE NOT NULL COMMENT '参考信号接收功率(dBm)',
    sinr DOUBLE NOT NULL COMMENT '信噪比(dB)',
    test_time DATETIME NOT NULL COMMENT '测试时间',
    update_time DATETIME NOT NULL COMMENT '记录更新时间',
    INDEX idx_test_time (test_time),
    INDEX idx_mac_throughput (mac_throughput)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='5G测试数据';

-- 创建峰值速率历史表
CREATE TABLE IF NOT EXISTS peak_rate_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mode VARCHAR(10) NOT NULL COMMENT '计算模式(FDD/TDD)',
    frame_type VARCHAR(20) COMMENT 'TDD帧结构类型',
    bandwidth DOUBLE NOT NULL COMMENT '系统带宽(MHz)',
    dl_slots INT NOT NULL COMMENT '下行时隙数',
    special_slots INT COMMENT '特殊时隙数(仅TDD模式)',
    peak_rate DOUBLE NOT NULL COMMENT '峰值速率(Gbit/s)',
    create_time DATETIME NOT NULL COMMENT '记录创建时间',
    update_time DATETIME NOT NULL COMMENT '记录更新时间',
    INDEX idx_create_time (create_time),
    INDEX idx_mode (mode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='5G峰值速率计算历史';

-- 初始化默认数据（可选）
INSERT INTO test_data (bler, create_time, mac_throughput, mcs, mimo_rank, rb_num, rsrp, sinr, test_time, update_time)
SELECT 1.5, NOW(), 100.0, 15, 2, 50, -85.0, 15.0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM test_data LIMIT 1); 