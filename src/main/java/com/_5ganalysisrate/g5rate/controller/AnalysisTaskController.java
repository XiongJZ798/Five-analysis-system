package com._5ganalysisrate.g5rate.controller;

import com._5ganalysisrate.g5rate.dto.ApiResponse;
import com._5ganalysisrate.g5rate.dto.ai.AnalysisTaskDTO;
import com._5ganalysisrate.g5rate.dto.ai.FeedbackRequest;
import com._5ganalysisrate.g5rate.model.ai.AnalysisTask;
import com._5ganalysisrate.g5rate.service.ai.AnalysisTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI驱动分析任务控制器
 * 提供任务创建、进度查询、规则/AI结果查询、报告生成、反馈等接口
 */
@Slf4j
@RestController
@RequestMapping("/analysis/tasks")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true", maxAge = 3600)
public class AnalysisTaskController {

    private final AnalysisTaskService analysisTaskService;

    /**
     * 创建分析任务
     * POST /api/analysis/tasks
     * Body: { "fileName": "xxx.xlsx", "taskType": "RULE_AI" }
     */
    @PostMapping
    public ApiResponse<AnalysisTaskDTO> createTask(@RequestBody Map<String, String> request) {
        try {
            String fileName = request.get("fileName");
            if (fileName == null || fileName.isBlank()) {
                return ApiResponse.error(400, "fileName不能为空");
            }
            String taskTypeStr = request.getOrDefault("taskType", "RULE_AI");
            AnalysisTask.TaskType taskType;
            try {
                taskType = AnalysisTask.TaskType.valueOf(taskTypeStr);
            } catch (IllegalArgumentException e) {
                return ApiResponse.error(400, "无效的taskType: " + taskTypeStr);
            }

            AnalysisTaskDTO task = analysisTaskService.createTask(fileName, taskType);
            // 异步触发执行
            analysisTaskService.executeTask(task.getId());
            return ApiResponse.success(task);
        } catch (Exception e) {
            log.error("创建分析任务失败", e);
            return ApiResponse.error("创建分析任务失败: " + e.getMessage());
        }
    }

    /**
     * 查询任务状态和进度
     * GET /api/analysis/tasks/{taskId}
     */
    @GetMapping("/{taskId}")
    public ApiResponse<AnalysisTaskDTO> getTask(@PathVariable Long taskId) {
        try {
            return ApiResponse.success(analysisTaskService.getTask(taskId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("查询任务失败: taskId={}", taskId, e);
            return ApiResponse.error("查询任务失败: " + e.getMessage());
        }
    }

    /**
     * 按文件名查询任务列表
     * GET /api/analysis/tasks?fileName=xxx
     */
    @GetMapping
    public ApiResponse<List<AnalysisTaskDTO>> getTasksByFileName(
            @RequestParam(required = false) String fileName) {
        try {
            if (fileName == null || fileName.isBlank()) {
                return ApiResponse.error(400, "fileName参数不能为空");
            }
            return ApiResponse.success(analysisTaskService.getTasksByFileName(fileName));
        } catch (Exception e) {
            log.error("查询任务列表失败", e);
            return ApiResponse.error("查询任务列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取规则分析结果
     * GET /api/analysis/tasks/{taskId}/rule-result
     */
    @GetMapping("/{taskId}/rule-result")
    public ApiResponse<Map<String, Object>> getRuleResult(@PathVariable Long taskId) {
        try {
            return ApiResponse.success(analysisTaskService.getRuleResult(taskId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("获取规则分析结果失败: taskId={}", taskId, e);
            return ApiResponse.error("获取规则分析结果失败: " + e.getMessage());
        }
    }

    /**
     * 获取AI分析结果
     * GET /api/analysis/tasks/{taskId}/ai-result
     */
    @GetMapping("/{taskId}/ai-result")
    public ApiResponse<Map<String, Object>> getAiResult(@PathVariable Long taskId) {
        try {
            return ApiResponse.success(analysisTaskService.getAiResult(taskId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("获取AI分析结果失败: taskId={}", taskId, e);
            return ApiResponse.error("获取AI分析结果失败: " + e.getMessage());
        }
    }

    /**
     * 生成综合报告
     * GET /api/analysis/tasks/{taskId}/report
     */
    @GetMapping("/{taskId}/report")
    public ApiResponse<Map<String, Object>> generateReport(@PathVariable Long taskId) {
        try {
            return ApiResponse.success(analysisTaskService.generateReport(taskId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("生成报告失败: taskId={}", taskId, e);
            return ApiResponse.error("生成报告失败: " + e.getMessage());
        }
    }

    /**
     * 提交反馈
     * POST /api/analysis/tasks/{taskId}/feedback
     */
    @PostMapping("/{taskId}/feedback")
    public ApiResponse<?> submitFeedback(@PathVariable Long taskId,
                                          @RequestBody FeedbackRequest request) {
        try {
            if (request.getFeedbackType() == null) {
                return ApiResponse.error(400, "feedbackType不能为空");
            }
            return ApiResponse.success(analysisTaskService.submitFeedback(taskId, request));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("提交反馈失败: taskId={}", taskId, e);
            return ApiResponse.error("提交反馈失败: " + e.getMessage());
        }
    }
}
