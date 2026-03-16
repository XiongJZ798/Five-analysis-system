package com._5ganalysisrate.g5rate.model.ai;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "analysis_result_rule")
public class AnalysisResultRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "kpi_summary_json", columnDefinition = "MEDIUMTEXT")
    private String kpiSummaryJson;

    @Column(name = "timeseries_json", columnDefinition = "MEDIUMTEXT")
    private String timeseriesJson;

    @Column(name = "distribution_json", columnDefinition = "MEDIUMTEXT")
    private String distributionJson;

    @Column(name = "peak_rate_json", columnDefinition = "MEDIUMTEXT")
    private String peakRateJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
