-- 清空测试数据
DELETE FROM test_data;
DELETE FROM peak_rate_history;

-- 插入测试数据
INSERT INTO test_data (test_time, rsrp, sinr, mac_throughput, mimo_rank, mcs, rb_num, bler, create_time, update_time)
VALUES 
    (NOW(), -85.0, 15.0, 100.0, 2, 15, 50, 1.5, NOW(), NOW()),
    (NOW() + INTERVAL 1 HOUR, -90.0, 12.0, 80.0, 1, 12, 40, 2.0, NOW(), NOW()),
    (NOW() + INTERVAL 2 HOUR, -88.0, 13.5, 90.0, 2, 14, 45, 1.8, NOW(), NOW());

-- 插入峰值速率计算历史记录
INSERT INTO peak_rate_history (mode, frame_type, bandwidth, dl_slots, special_slots, peak_rate, create_time, update_time)
VALUES 
    ('FDD', NULL, 100.0, 10, NULL, 2.5, NOW(), NOW()),
    ('TDD', 'FRAME1', 100.0, 8, 2, 2.0, NOW(), NOW()); 