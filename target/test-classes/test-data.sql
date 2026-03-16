-- 清空测试数据
DELETE FROM test_data;

-- 插入测试数据
INSERT INTO test_data (test_time, rsrp, sinr, mac_throughput, mimo_rank, mcs, prb_num, bler, create_time, update_time)
VALUES 
    (NOW(), -85.0, 15.0, 100.0, 2, 15, 50, 1.5, NOW(), NOW()),
    (DATEADD('HOUR', 1, NOW()), -90.0, 12.0, 80.0, 1, 12, 40, 2.0, NOW(), NOW()),
    (DATEADD('HOUR', 2, NOW()), -88.0, 13.5, 90.0, 2, 14, 45, 1.8, NOW(), NOW());
