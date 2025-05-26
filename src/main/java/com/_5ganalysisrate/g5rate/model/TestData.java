package com._5ganalysisrate.g5rate.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 5G测试数据实体类
 * 用于存储从Excel导入的测试数据
 */
@Data
@Entity
@Table(name = "test_data")
public class TestData {
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    /**
     * 块错误率(Block Error Rate)
     * 单位: %
     * 范围: 0-100
     */
    @Column(name = "bler")
    private Double bler;
    
    /**
     * 记录创建时间
     */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
    
    /**
     * MAC层下行速率
     * 单位: Mbps
     */
    @Column(name = "mac_throughput")
    private Double macThroughput;
    
    /**
     * 调制编码方案(Modulation and Coding Scheme)
     * 范围: 0-28
     */
    @Column(name = "mcs")
    private Double mcs;
    
    /**
     * MIMO传输层数(Rank)
     * 范围: 1-8
     */
    @Column(name = "mimo_rank")
    private Integer rank;
    
    /**
     * 物理资源块数量(Physical Resource Block Number)
     * 范围: 0-275
     */
    @Column(name = "prb_num")
    private Integer prbNum;
    
    /**
     * 参考信号接收功率(Reference Signal Received Power)
     * 单位: dBm
     * 范围: -140dBm到-44dBm
     */
    @Column(name = "rsrp")
    private Double rsrp;
    
    /**
     * 信噪比(Signal to Interference plus Noise Ratio)
     * 单位: dB
     * 范围: -20dB到40dB
     */
    @Column(name = "sinr")
    private Double sinr;
    
    /**
     * 测试时间
     * 精确到毫秒
     */
    @Column(name = "test_time", nullable = false)
    private LocalDateTime testTime;
    
    /**
     * 记录更新时间
     */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createTime == null) {
            createTime = now;
        }
        if (updateTime == null) {
            updateTime = now;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
} 