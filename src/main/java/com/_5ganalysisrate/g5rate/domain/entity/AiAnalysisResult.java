package com._5ganalysisrate.g5rate.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 分析结果实体
 */
@Data
@Entity
@Table(name = "analysis_result_ai")
public class AiAnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联任务ID */
    @Column(name = "task_id", nullable = false)
    private Long taskId;

    /** 模型名称 */
    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    /** 提示词版本 */
    @Column(name = "prompt_version", nullable = false, length = 50)
    private String promptVersion;

    /** 输入摘要 */
    @Column(name = "input_digest", length = 64)
    private String inputDigest;

    /** AI 结构化输出 JSON */
    @Column(name = "output_json", columnDefinition = "LONGTEXT")
    private String outputJson;

    /** 置信度得分 */
    @Column(name = "confidence_score")
    private Double confidenceScore;

    /** Token 用量 */
    @Column(name = "token_usage")
    private Integer tokenUsage;

    /** 调用耗时 ms */
    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
