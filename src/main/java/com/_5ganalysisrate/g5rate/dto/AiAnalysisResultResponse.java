package com._5ganalysisrate.g5rate.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 分析结果响应 DTO
 * 对应 AI 严格输出 Schema
 */
@Data
public class AiAnalysisResultResponse {

    private Long resultId;
    private Long taskId;

    private String modelName;
    private String promptVersion;
    private Double confidenceScore;
    private Integer tokenUsage;
    private Long latencyMs;

    /** AI 总结 */
    private String summary;

    /** 发现列表 */
    private List<Map<String, Object>> findings;

    /** 根因分析 */
    private List<Map<String, Object>> rootCauses;

    /** 优化动作 */
    private List<Map<String, Object>> actions;

    /** 3GPP 合规检查 */
    private Map<String, Object> complianceCheck;

    /** 分析局限性 */
    private List<String> limitations;

    private LocalDateTime createdAt;
}
