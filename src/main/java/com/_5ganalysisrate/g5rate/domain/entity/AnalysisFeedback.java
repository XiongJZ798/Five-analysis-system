package com._5ganalysisrate.g5rate.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工程师反馈实体（用于闭环学习）
 */
@Data
@Entity
@Table(name = "analysis_feedback")
public class AnalysisFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联任务ID */
    @Column(name = "task_id", nullable = false)
    private Long taskId;

    /** 关联发现ID (如 F1) */
    @Column(name = "finding_id", length = 50)
    private String findingId;

    /** 反馈类型: ACCEPT / REJECT / PARTIAL */
    @Column(name = "feedback_type", nullable = false, length = 20)
    private String feedbackType;

    /** 备注 */
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    /** 反馈人 */
    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
