package com._5ganalysisrate.g5rate.controller;

import com._5ganalysisrate.g5rate.domain.entity.AnalysisFeedback;
import com._5ganalysisrate.g5rate.domain.entity.AnalysisTask;
import com._5ganalysisrate.g5rate.dto.*;
import com._5ganalysisrate.g5rate.service.AnalysisTaskService;
import com._5ganalysisrate.g5rate.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * 分析任务 Controller
 *
 * <pre>
 * POST   /analysis/tasks                       创建任务
 * GET    /analysis/tasks/{taskId}              查询任务状态
 * GET    /analysis/tasks/{taskId}/rule-result  查询规则分析结果
 * GET    /analysis/tasks/{taskId}/ai-result    查询 AI 分析结果
 * GET    /analysis/tasks/{taskId}/report       获取合并报告
 * POST   /analysis/tasks/{taskId}/feedback     提交工程师反馈
 * </pre>
 */
@RestController
@RequestMapping("/analysis/tasks")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true", maxAge = 3600)
public class AnalysisTaskController {

    private static final Logger log = LoggerFactory.getLogger(AnalysisTaskController.class);

    private final AnalysisTaskService analysisTaskService;
    private final ReportService reportService;

    /**
     * 创建分析任务
     * POST /analysis/tasks
     */
    @PostMapping
    public ApiResponse<AnalysisTaskResponse> createTask(@RequestBody CreateAnalysisTaskRequest request) {
        try {
            if (request.getFileId() == null || request.getFileId().isBlank()) {
                return ApiResponse.error(400, "fileId 不能为空");
            }
            AnalysisTask task = analysisTaskService.createTask(request);
            log.info("[TaskController] 创建任务成功, taskId={}", task.getId());
            return ApiResponse.success(reportService.buildTaskResponse(task));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("[TaskController] 创建任务失败", e);
            return ApiResponse.error("创建分析任务失败: " + e.getMessage());
        }
    }

    /**
     * 查询任务状态与进度
     * GET /analysis/tasks/{taskId}
     */
    @GetMapping("/{taskId}")
    public ApiResponse<AnalysisTaskResponse> getTask(@PathVariable Long taskId) {
        try {
            AnalysisTask task = requireTask(taskId);
            return ApiResponse.success(reportService.buildTaskResponse(task));
        } catch (ResponseStatusException e) {
            return ApiResponse.error(e.getStatusCode().value(), e.getReason());
        } catch (Exception e) {
            log.error("[TaskController] 查询任务失败, taskId={}", taskId, e);
            return ApiResponse.error("查询任务失败: " + e.getMessage());
        }
    }

    /**
     * 获取规则分析结果
     * GET /analysis/tasks/{taskId}/rule-result
     */
    @GetMapping("/{taskId}/rule-result")
    public ApiResponse<RuleAnalysisResultResponse> getRuleResult(@PathVariable Long taskId) {
        try {
            requireTask(taskId);
            return reportService.buildRuleResultResponse(taskId)
                    .map(ApiResponse::success)
                    .orElseGet(() -> ApiResponse.error(404, "规则分析结果尚未生成"));
        } catch (ResponseStatusException e) {
            return ApiResponse.error(e.getStatusCode().value(), e.getReason());
        } catch (Exception e) {
            log.error("[TaskController] 获取规则结果失败, taskId={}", taskId, e);
            return ApiResponse.error("获取规则分析结果失败: " + e.getMessage());
        }
    }

    /**
     * 获取 AI 分析结果
     * GET /analysis/tasks/{taskId}/ai-result
     */
    @GetMapping("/{taskId}/ai-result")
    public ApiResponse<AiAnalysisResultResponse> getAiResult(@PathVariable Long taskId) {
        try {
            requireTask(taskId);
            return reportService.buildAiResultResponse(taskId)
                    .map(ApiResponse::success)
                    .orElseGet(() -> ApiResponse.error(404, "AI 分析结果尚未生成"));
        } catch (ResponseStatusException e) {
            return ApiResponse.error(e.getStatusCode().value(), e.getReason());
        } catch (Exception e) {
            log.error("[TaskController] 获取AI结果失败, taskId={}", taskId, e);
            return ApiResponse.error("获取 AI 分析结果失败: " + e.getMessage());
        }
    }

    /**
     * 获取合并报告
     * GET /analysis/tasks/{taskId}/report
     */
    @GetMapping("/{taskId}/report")
    public ApiResponse<AnalysisReportResponse> getReport(@PathVariable Long taskId) {
        try {
            AnalysisTask task = requireTask(taskId);
            AnalysisReportResponse report = reportService.buildReport(task);
            return ApiResponse.success(report);
        } catch (ResponseStatusException e) {
            return ApiResponse.error(e.getStatusCode().value(), e.getReason());
        } catch (Exception e) {
            log.error("[TaskController] 获取报告失败, taskId={}", taskId, e);
            return ApiResponse.error("获取报告失败: " + e.getMessage());
        }
    }

    /**
     * 提交工程师反馈
     * POST /analysis/tasks/{taskId}/feedback
     */
    @PostMapping("/{taskId}/feedback")
    public ApiResponse<Long> submitFeedback(@PathVariable Long taskId,
                                             @RequestBody CreateFeedbackRequest request) {
        try {
            AnalysisFeedback feedback = analysisTaskService.submitFeedback(taskId, request);
            log.info("[TaskController] 反馈提交成功, taskId={}, feedbackId={}", taskId, feedback.getId());
            return ApiResponse.success(feedback.getId());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (ResponseStatusException e) {
            return ApiResponse.error(e.getStatusCode().value(), e.getReason());
        } catch (Exception e) {
            log.error("[TaskController] 提交反馈失败, taskId={}", taskId, e);
            return ApiResponse.error("提交反馈失败: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------

    private AnalysisTask requireTask(Long taskId) {
        return analysisTaskService.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在: " + taskId));
    }
}
