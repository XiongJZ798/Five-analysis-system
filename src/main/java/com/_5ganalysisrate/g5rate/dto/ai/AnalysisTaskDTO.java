package com._5ganalysisrate.g5rate.dto.ai;

import com._5ganalysisrate.g5rate.model.ai.AnalysisTask;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AnalysisTaskDTO {
    private Long id;
    private String fileName;
    private AnalysisTask.TaskType taskType;
    private AnalysisTask.TaskStatus status;
    private int progress;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
