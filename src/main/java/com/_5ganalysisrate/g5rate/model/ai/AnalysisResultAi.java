package com._5ganalysisrate.g5rate.model.ai;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "analysis_result_ai")
public class AnalysisResultAi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "prompt_version")
    private String promptVersion;

    @Column(name = "input_digest")
    private String inputDigest;

    @Column(name = "output_json", columnDefinition = "MEDIUMTEXT")
    private String outputJson;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "token_usage")
    private Integer tokenUsage;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
