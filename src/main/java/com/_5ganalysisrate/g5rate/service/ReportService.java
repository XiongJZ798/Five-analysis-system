package com._5ganalysisrate.g5rate.service;

import com._5ganalysisrate.g5rate.domain.entity.AiAnalysisResult;
import com._5ganalysisrate.g5rate.domain.entity.AnalysisTask;
import com._5ganalysisrate.g5rate.domain.entity.RuleAnalysisResult;
import com._5ganalysisrate.g5rate.dto.AiAnalysisResultResponse;
import com._5ganalysisrate.g5rate.dto.AnalysisReportResponse;
import com._5ganalysisrate.g5rate.dto.AnalysisTaskResponse;
import com._5ganalysisrate.g5rate.dto.RuleAnalysisResultResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 报告整合服务
 * 将规则分析与 AI 分析结果合并为统一报告 DTO，适合前端展示、Markdown/PDF 导出
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final AnalysisTaskService analysisTaskService;
    private final RuleAnalysisService ruleAnalysisService;
    private final AiAnalysisService aiAnalysisService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构建任务状态响应 DTO
     */
    public AnalysisTaskResponse buildTaskResponse(AnalysisTask task) {
        AnalysisTaskResponse resp = new AnalysisTaskResponse();
        resp.setTaskId(task.getId());
        resp.setUploadFileId(task.getUploadFileId());
        resp.setTaskType(task.getTaskType());
        resp.setStatus(task.getStatus());
        resp.setProgress(task.getProgress());
        resp.setErrorMessage(task.getErrorMessage());
        resp.setCreatedBy(task.getCreatedBy());
        resp.setCreatedAt(task.getCreatedAt());
        resp.setUpdatedAt(task.getUpdatedAt());
        return resp;
    }

    /**
     * 构建规则分析结果响应 DTO
     */
    public Optional<RuleAnalysisResultResponse> buildRuleResultResponse(Long taskId) {
        return ruleAnalysisService.findByTaskId(taskId)
                .map(this::toRuleResultResponse);
    }

    /**
     * 构建 AI 分析结果响应 DTO
     */
    public Optional<AiAnalysisResultResponse> buildAiResultResponse(Long taskId) {
        return aiAnalysisService.findByTaskId(taskId)
                .map(this::toAiResultResponse);
    }

    /**
     * 构建合并报告
     */
    public AnalysisReportResponse buildReport(AnalysisTask task) {
        AnalysisReportResponse report = new AnalysisReportResponse();
        report.setTaskId(task.getId());
        report.setUploadFileId(task.getUploadFileId());
        report.setTaskType(task.getTaskType());
        report.setStatus(task.getStatus());

        buildRuleResultResponse(task.getId()).ifPresent(report::setRuleResult);
        buildAiResultResponse(task.getId()).ifPresent(report::setAiResult);

        report.setGeneratedAt(LocalDateTime.now());
        return report;
    }

    // -------------------------------------------------------------------------

    private RuleAnalysisResultResponse toRuleResultResponse(RuleAnalysisResult entity) {
        RuleAnalysisResultResponse r = new RuleAnalysisResultResponse();
        r.setResultId(entity.getId());
        r.setTaskId(entity.getTaskId());
        r.setKpiSummary(parseJsonToMap(entity.getKpiSummaryJson()));
        r.setTimeseries(parseJsonToMap(entity.getTimeseriesJson()));
        r.setDistribution(parseJsonToMap(entity.getDistributionJson()));
        r.setPeakRate(parseJsonToMap(entity.getPeakRateJson()));
        r.setCreatedAt(entity.getCreatedAt());
        return r;
    }

    @SuppressWarnings("unchecked")
    private AiAnalysisResultResponse toAiResultResponse(AiAnalysisResult entity) {
        AiAnalysisResultResponse r = new AiAnalysisResultResponse();
        r.setResultId(entity.getId());
        r.setTaskId(entity.getTaskId());
        r.setModelName(entity.getModelName());
        r.setPromptVersion(entity.getPromptVersion());
        r.setConfidenceScore(entity.getConfidenceScore());
        r.setTokenUsage(entity.getTokenUsage());
        r.setLatencyMs(entity.getLatencyMs());
        r.setCreatedAt(entity.getCreatedAt());

        if (entity.getOutputJson() != null) {
            try {
                Map<String, Object> parsed = objectMapper.readValue(entity.getOutputJson(), Map.class);
                r.setSummary((String) parsed.get("summary"));
                r.setFindings((List<Map<String, Object>>) parsed.get("findings"));
                r.setRootCauses((List<Map<String, Object>>) parsed.get("root_causes"));
                r.setActions((List<Map<String, Object>>) parsed.get("actions"));
                r.setComplianceCheck((Map<String, Object>) parsed.get("compliance_check"));
                r.setLimitations((List<String>) parsed.get("limitations"));
            } catch (Exception e) {
                log.warn("[ReportService] AI输出JSON解析失败: {}", e.getMessage());
            }
        }

        return r;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonToMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of("raw", json);
        }
    }
}
