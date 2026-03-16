package com._5ganalysisrate.g5rate.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 规则分析结果实体
 */
@Data
@Entity
@Table(name = "analysis_result_rule")
public class RuleAnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联任务ID */
    @Column(name = "task_id", nullable = false)
    private Long taskId;

    /** KPI汇总 JSON */
    @Column(name = "kpi_summary_json", columnDefinition = "LONGTEXT")
    private String kpiSummaryJson;

    /** 时序分析结果 JSON */
    @Column(name = "timeseries_json", columnDefinition = "LONGTEXT")
    private String timeseriesJson;

    /** 速率分布 JSON */
    @Column(name = "distribution_json", columnDefinition = "LONGTEXT")
    private String distributionJson;

    /** 峰值速率计算结果 JSON */
    @Column(name = "peak_rate_json", columnDefinition = "LONGTEXT")
    private String peakRateJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
