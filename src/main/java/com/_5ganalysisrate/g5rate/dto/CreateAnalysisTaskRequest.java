package com._5ganalysisrate.g5rate.dto;

import com._5ganalysisrate.g5rate.domain.enums.TaskType;
import lombok.Data;

/**
 * 创建分析任务请求 DTO
 */
@Data
public class CreateAnalysisTaskRequest {

    /** 上传文件标识 */
    private String fileId;

    /**
     * 任务类型: RULE_ONLY 或 RULE_AI
     * 默认为 RULE_AI
     */
    private TaskType taskType = TaskType.RULE_AI;
}
