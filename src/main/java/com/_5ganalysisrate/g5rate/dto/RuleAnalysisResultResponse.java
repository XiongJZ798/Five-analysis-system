package com._5ganalysisrate.g5rate.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 规则分析结果响应 DTO
 */
@Data
public class RuleAnalysisResultResponse {

    private Long resultId;
    private Long taskId;

    /** KPI 统计汇总 */
    private Map<String, Object> kpiSummary;

    /** 时序分析结果 */
    private Map<String, Object> timeseries;

    /** 速率分布数据 */
    private Map<String, Object> distribution;

    /** 峰值速率计算结果 */
    private Map<String, Object> peakRate;

    private LocalDateTime createdAt;
}
