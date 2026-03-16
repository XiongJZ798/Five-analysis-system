package com._5ganalysisrate.g5rate.service.ai;

import com._5ganalysisrate.g5rate.dto.ai.AiAnalysisResultDTO;
import com._5ganalysisrate.g5rate.dto.ai.AnalysisTaskDTO;
import com._5ganalysisrate.g5rate.dto.ai.FeedbackRequest;
import com._5ganalysisrate.g5rate.model.ai.*;
import com._5ganalysisrate.g5rate.model.TestData;
import com._5ganalysisrate.g5rate.repository.TestDataRepository;
import com._5ganalysisrate.g5rate.repository.ai.*;
import com._5ganalysisrate.g5rate.service.AnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 分析任务编排服务
 * 负责任务生命周期管理、规则分析触发、AI分析调用和状态流转
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisTaskService {

    private final AnalysisTaskRepository taskRepository;
    private final AnalysisResultRuleRepository ruleResultRepository;
    private final AnalysisResultAiRepository aiResultRepository;
    private final AnalysisFeedbackRepository feedbackRepository;
    private final AnalysisService analysisService;
    private final TestDataRepository testDataRepository;
    private final AiAnalysisService aiAnalysisService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建分析任务（同步创建，异步执行）
     */
    @Transactional
    public AnalysisTaskDTO createTask(String fileName, AnalysisTask.TaskType taskType) {
        AnalysisTask task = new AnalysisTask();
        task.setFileName(fileName);
        task.setTaskType(taskType == null ? AnalysisTask.TaskType.RULE_AI : taskType);
        task.setStatus(AnalysisTask.TaskStatus.PENDING);
        task.setProgress(0);
        task = taskRepository.save(task);
        log.info("创建分析任务: id={}, fileName={}, type={}", task.getId(), fileName, task.getTaskType());
        return toDTO(task);
    }

    /**
     * 异步执行分析任务
     */
    @Async
    public void executeTask(Long taskId) {
        log.info("开始执行分析任务: taskId={}", taskId);
        AnalysisTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + taskId));

        updateStatus(task, AnalysisTask.TaskStatus.RUNNING, 10, null);

        boolean ruleSuccess = false;
        boolean aiSuccess = false;
        String errorMsg = null;

        // 步骤1：规则分析
        try {
            log.info("执行规则分析: taskId={}", taskId);
            Map<String, Object> ruleResult = runRuleAnalysis(task);
            saveRuleResult(taskId, ruleResult);
            ruleSuccess = true;
            updateProgress(task, 60);
            log.info("规则分析完成: taskId={}", taskId);
        } catch (Exception e) {
            log.error("规则分析失败: taskId={}", taskId, e);
            errorMsg = "规则分析失败: " + e.getMessage();
            updateStatus(task, AnalysisTask.TaskStatus.FAILED, task.getProgress(), errorMsg);
            return;
        }

        // 步骤2：AI分析（仅RULE_AI模式）
        if (task.getTaskType() == AnalysisTask.TaskType.RULE_AI) {
            try {
                log.info("执行AI分析: taskId={}", taskId);
                long aiStart = System.currentTimeMillis();
                Map<String, Object> kpiSummary = buildKpiSummary();
                Map<String, Object> ruleResultMap = getRuleResultMap(taskId);
                AiAnalysisResultDTO aiResult = aiAnalysisService.analyze(
                        task.getFileName(), kpiSummary, ruleResultMap);
                long latency = System.currentTimeMillis() - aiStart;
                saveAiResult(taskId, aiResult, task.getFileName(), kpiSummary, latency);
                aiSuccess = true;
                updateProgress(task, 90);
                log.info("AI分析完成: taskId={}, latency={}ms", taskId, latency);
            } catch (Exception e) {
                log.warn("AI分析失败(不影响规则结果): taskId={}", taskId, e);
                errorMsg = "AI分析失败: " + e.getMessage();
                aiSuccess = false;
            }
        }

        // 确定最终状态
        AnalysisTask.TaskStatus finalStatus;
        if (task.getTaskType() == AnalysisTask.TaskType.RULE_ONLY) {
            finalStatus = AnalysisTask.TaskStatus.SUCCESS;
        } else if (ruleSuccess && aiSuccess) {
            finalStatus = AnalysisTask.TaskStatus.SUCCESS;
        } else if (ruleSuccess) {
            finalStatus = AnalysisTask.TaskStatus.PARTIAL_SUCCESS;
        } else {
            finalStatus = AnalysisTask.TaskStatus.FAILED;
        }

        updateStatus(task, finalStatus, 100, errorMsg);
        log.info("分析任务完成: taskId={}, status={}", taskId, finalStatus);
    }

    @Transactional(readOnly = true)
    public AnalysisTaskDTO getTask(Long taskId) {
        return taskRepository.findById(taskId)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + taskId));
    }

    @Transactional(readOnly = true)
    public List<AnalysisTaskDTO> getTasksByFileName(String fileName) {
        return taskRepository.findByFileNameOrderByCreatedAtDesc(fileName)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRuleResult(Long taskId) {
        return ruleResultRepository.findByTaskId(taskId)
                .map(r -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("taskId", taskId);
                    result.put("kpiSummary", parseJson(r.getKpiSummaryJson()));
                    result.put("timeseries", parseJson(r.getTimeseriesJson()));
                    result.put("distribution", parseJson(r.getDistributionJson()));
                    result.put("peakRate", parseJson(r.getPeakRateJson()));
                    result.put("createdAt", r.getCreatedAt());
                    return result;
                })
                .orElseThrow(() -> new IllegalArgumentException("规则分析结果不存在: taskId=" + taskId));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAiResult(Long taskId) {
        return aiResultRepository.findByTaskId(taskId)
                .map(r -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("taskId", taskId);
                    result.put("modelName", r.getModelName());
                    result.put("promptVersion", r.getPromptVersion());
                    result.put("confidenceScore", r.getConfidenceScore());
                    result.put("tokenUsage", r.getTokenUsage());
                    result.put("latencyMs", r.getLatencyMs());
                    result.put("analysis", parseJson(r.getOutputJson()));
                    result.put("createdAt", r.getCreatedAt());
                    return result;
                })
                .orElseThrow(() -> new IllegalArgumentException("AI分析结果不存在: taskId=" + taskId));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> generateReport(Long taskId) {
        AnalysisTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + taskId));

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("taskId", taskId);
        report.put("fileName", task.getFileName());
        report.put("status", task.getStatus());
        report.put("generatedAt", java.time.LocalDateTime.now());

        // 规则分析结果
        ruleResultRepository.findByTaskId(taskId).ifPresent(r ->
                report.put("ruleAnalysis", Map.of(
                        "kpiSummary", parseJson(r.getKpiSummaryJson()),
                        "distribution", parseJson(r.getDistributionJson()),
                        "peakRate", parseJson(r.getPeakRateJson())
                ))
        );

        // AI分析结果
        aiResultRepository.findByTaskId(taskId).ifPresent(r ->
                report.put("aiAnalysis", parseJson(r.getOutputJson()))
        );

        // 反馈汇总
        List<AnalysisFeedback> feedbacks = feedbackRepository.findByTaskIdOrderByCreatedAtDesc(taskId);
        if (!feedbacks.isEmpty()) {
            report.put("feedbackCount", feedbacks.size());
        }

        return report;
    }

    @Transactional
    public AnalysisFeedback submitFeedback(Long taskId, FeedbackRequest request) {
        taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + taskId));

        AnalysisFeedback feedback = new AnalysisFeedback();
        feedback.setTaskId(taskId);
        feedback.setFindingId(request.getFindingId());
        feedback.setFeedbackType(request.getFeedbackType());
        feedback.setComment(request.getComment());
        feedback.setCreatedBy(request.getCreatedBy());
        return feedbackRepository.save(feedback);
    }

    // ===== 私有辅助方法 =====

    private Map<String, Object> runRuleAnalysis(AnalysisTask task) {
        Map<String, Object> result = new LinkedHashMap<>();
        // 时序分析
        result.put("timeseries", analysisService.getTimeSeriesData(
                List.of("rsrp", "sinr", "macThroughput")));
        // 分布分析
        result.put("distribution", analysisService.getRateDistribution(null));
        // KPI汇总
        result.put("kpiSummary", buildKpiSummary());
        return result;
    }

    private Map<String, Object> buildKpiSummary() {
        List<TestData> data = testDataRepository.findAllByOrderByTestTimeAsc();
        if (data.isEmpty()) {
            return Map.of("dataCount", 0);
        }

        DoubleSummaryStatistics throughputStats = data.stream()
                .filter(d -> d.getMacThroughput() != null)
                .mapToDouble(TestData::getMacThroughput)
                .summaryStatistics();

        DoubleSummaryStatistics rsrpStats = data.stream()
                .filter(d -> d.getRsrp() != null)
                .mapToDouble(TestData::getRsrp)
                .summaryStatistics();

        DoubleSummaryStatistics sinrStats = data.stream()
                .filter(d -> d.getSinr() != null)
                .mapToDouble(TestData::getSinr)
                .summaryStatistics();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("dataCount", data.size());
        if (throughputStats.getCount() > 0) {
            summary.put("avgMacThroughput", Math.round(throughputStats.getAverage() * 100) / 100.0);
            summary.put("maxMacThroughput", throughputStats.getMax());
            summary.put("minMacThroughput", throughputStats.getMin());
        }
        if (rsrpStats.getCount() > 0) {
            summary.put("avgRsrp", Math.round(rsrpStats.getAverage() * 100) / 100.0);
        }
        if (sinrStats.getCount() > 0) {
            summary.put("avgSinr", Math.round(sinrStats.getAverage() * 100) / 100.0);
        }
        return summary;
    }

    private Map<String, Object> getRuleResultMap(Long taskId) {
        return ruleResultRepository.findByTaskId(taskId)
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("timeseries", parseJson(r.getTimeseriesJson()));
                    m.put("distribution", parseJson(r.getDistributionJson()));
                    return m;
                })
                .orElse(Collections.emptyMap());
    }

    @Transactional
    protected void saveRuleResult(Long taskId, Map<String, Object> ruleResult) {
        AnalysisResultRule result = new AnalysisResultRule();
        result.setTaskId(taskId);
        try {
            if (ruleResult.containsKey("kpiSummary")) {
                result.setKpiSummaryJson(objectMapper.writeValueAsString(ruleResult.get("kpiSummary")));
            }
            if (ruleResult.containsKey("timeseries")) {
                result.setTimeseriesJson(objectMapper.writeValueAsString(ruleResult.get("timeseries")));
            }
            if (ruleResult.containsKey("distribution")) {
                result.setDistributionJson(objectMapper.writeValueAsString(ruleResult.get("distribution")));
            }
        } catch (Exception e) {
            log.error("序列化规则分析结果失败", e);
        }
        ruleResultRepository.save(result);
    }

    @Transactional
    protected void saveAiResult(Long taskId, AiAnalysisResultDTO aiResult, String fileName,
                                 Map<String, Object> kpiSummary, long latencyMs) {
        AnalysisResultAi result = new AnalysisResultAi();
        result.setTaskId(taskId);
        result.setModelName("mock-ai");
        result.setPromptVersion("v1.0");
        result.setInputDigest(aiAnalysisService.computeDigest(fileName, kpiSummary));
        result.setLatencyMs(latencyMs);
        if (aiResult.getFindings() != null && !aiResult.getFindings().isEmpty()) {
            // 计算平均置信度
            double avgConf = aiResult.getRootCauses() != null
                    ? aiResult.getRootCauses().stream()
                    .filter(rc -> rc.getConfidence() != null)
                    .mapToDouble(AiAnalysisResultDTO.RootCause::getConfidence)
                    .average().orElse(0.75)
                    : 0.75;
            result.setConfidenceScore(avgConf);
        }
        try {
            result.setOutputJson(objectMapper.writeValueAsString(aiResult));
        } catch (Exception e) {
            log.error("序列化AI分析结果失败", e);
        }
        aiResultRepository.save(result);
    }

    private void updateStatus(AnalysisTask task, AnalysisTask.TaskStatus status,
                               int progress, String errorMessage) {
        task.setStatus(status);
        task.setProgress(progress);
        task.setErrorMessage(errorMessage);
        taskRepository.save(task);
    }

    private void updateProgress(AnalysisTask task, int progress) {
        task.setProgress(progress);
        taskRepository.save(task);
    }

    private Object parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }

    private AnalysisTaskDTO toDTO(AnalysisTask task) {
        AnalysisTaskDTO dto = new AnalysisTaskDTO();
        dto.setId(task.getId());
        dto.setFileName(task.getFileName());
        dto.setTaskType(task.getTaskType());
        dto.setStatus(task.getStatus());
        dto.setProgress(task.getProgress());
        dto.setErrorMessage(task.getErrorMessage());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setUpdatedAt(task.getUpdatedAt());
        return dto;
    }
}
