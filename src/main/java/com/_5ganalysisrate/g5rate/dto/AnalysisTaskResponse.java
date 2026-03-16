package com._5ganalysisrate.g5rate.dto;

import com._5ganalysisrate.g5rate.domain.enums.TaskStatus;
import com._5ganalysisrate.g5rate.domain.enums.TaskType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分析任务响应 DTO
 */
@Data
public class AnalysisTaskResponse {

    private Long taskId;
    private String uploadFileId;
    private TaskType taskType;
    private TaskStatus status;
    private Integer progress;
    private String errorMessage;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
