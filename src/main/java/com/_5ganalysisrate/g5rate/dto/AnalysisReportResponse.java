package com._5ganalysisrate.g5rate.dto;

import com._5ganalysisrate.g5rate.domain.enums.TaskStatus;
import com._5ganalysisrate.g5rate.domain.enums.TaskType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 合并报告响应 DTO（适合 Markdown/PDF 渲染）
 */
@Data
public class AnalysisReportResponse {

    private Long taskId;
    private String uploadFileId;
    private TaskType taskType;
    private TaskStatus status;

    /** 规则分析结果 */
    private RuleAnalysisResultResponse ruleResult;

    /** AI 分析结果（若任务类型为 RULE_AI 且 AI 分析成功） */
    private AiAnalysisResultResponse aiResult;

    private LocalDateTime generatedAt;
}
