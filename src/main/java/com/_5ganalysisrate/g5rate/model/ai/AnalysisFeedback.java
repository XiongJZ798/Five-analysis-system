package com._5ganalysisrate.g5rate.model.ai;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "analysis_feedback")
public class AnalysisFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "finding_id")
    private String findingId;

    @Column(name = "feedback_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private FeedbackType feedbackType;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public enum FeedbackType {
        ACCEPT, REJECT, PARTIAL
    }
}
