package com._5ganalysisrate.g5rate.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 5G小区单用户峰值速率计算历史记录
 */
@Data
@Entity
@Table(name = "peak_rate_history")
public class PeakRateHistory {
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    /**
     * 计算模式（FDD/TDD）
     */
    @Column(name = "mode", nullable = false, length = 10)
    private String mode;
    
    /**
     * TDD帧结构类型
     */
    @Column(name = "frame_type", length = 20)
    private String frameType;
    
    /**
     * 系统带宽(MHz)
     */
    @Column(name = "bandwidth", nullable = false)
    private Double bandwidth;
    
    /**
     * 下行时隙数
     */
    @Column(name = "dl_slots", nullable = false)
    private Integer dlSlots;
    
    /**
     * 特殊时隙数（仅TDD模式）
     */
    @Column(name = "special_slots")
    private Integer specialSlots;
    
    /**
     * 计算得出的峰值速率(Gbit/s)
     */
    @Column(name = "peak_rate", nullable = false)
    private Double peakRate;
    
    /**
     * 记录创建时间
     */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
    
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