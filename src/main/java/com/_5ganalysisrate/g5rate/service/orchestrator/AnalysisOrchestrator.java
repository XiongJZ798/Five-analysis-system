package com._5ganalysisrate.g5rate.service.orchestrator;

import com._5ganalysisrate.g5rate.domain.entity.AnalysisTask;
import com._5ganalysisrate.g5rate.domain.entity.RuleAnalysisResult;
import com._5ganalysisrate.g5rate.domain.enums.TaskStatus;
import com._5ganalysisrate.g5rate.domain.enums.TaskType;
import com._5ganalysisrate.g5rate.repository.AnalysisTaskRepository;
import com._5ganalysisrate.g5rate.service.AiAnalysisException;
import com._5ganalysisrate.g5rate.service.AiAnalysisService;
import com._5ganalysisrate.g5rate.service.RuleAnalysisService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 分析流水线编排器
 * 异步执行完整的分析生命周期：规则分析 -> AI 分析 -> 结果持久化
 *
 * 状态流转:
 *   PENDING -> RUNNING -> SUCCESS
 *                      -> PARTIAL_SUCCESS (规则成功，AI失败)
 *                      -> FAILED (规则失败)
 */
@Component
@RequiredArgsConstructor
public class AnalysisOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AnalysisOrchestrator.class);

    private final AnalysisTaskRepository analysisTaskRepository;
    private final RuleAnalysisService ruleAnalysisService;
    private final AiAnalysisService aiAnalysisService;

    /**
     * 异步触发完整分析流水线
     *
     * @param taskId 分析任务 ID
     */
    @Async("analysisTaskExecutor")
    public void executeAsync(Long taskId) {
        log.info("[Orchestrator] 开始执行分析任务, taskId={}", taskId);

        AnalysisTask task = analysisTaskRepository.findById(taskId).orElse(null);
        if (task == null) {
            log.error("[Orchestrator] 任务不存在, taskId={}", taskId);
            return;
        }

        // 设置为运行中
        updateStatus(task, TaskStatus.RUNNING, 0, null);

        // Step 1: 规则分析
        RuleAnalysisResult ruleResult;
        try {
            log.info("[Orchestrator] 执行规则分析, taskId={}", taskId);
            ruleResult = ruleAnalysisService.runAndPersist(taskId);
            updateProgress(task, 50);
            log.info("[Orchestrator] 规则分析完成, taskId={}", taskId);
        } catch (Exception e) {
            log.error("[Orchestrator] 规则分析失败, taskId={}: {}", taskId, e.getMessage(), e);
            updateStatus(task, TaskStatus.FAILED, 0, "规则分析失败: " + e.getMessage());
            return;
        }

        // Step 2: AI 分析（仅 RULE_AI 类型）
        if (TaskType.RULE_AI.equals(task.getTaskType())) {
            try {
                log.info("[Orchestrator] 执行 AI 分析, taskId={}", taskId);
                aiAnalysisService.runAndPersist(taskId, ruleResult);
                updateStatus(task, TaskStatus.SUCCESS, 100, null);
                log.info("[Orchestrator] AI 分析完成, 任务成功, taskId={}", taskId);
            } catch (AiAnalysisException e) {
                log.warn("[Orchestrator] AI 分析失败，规则分析结果可用, taskId={}: {}", taskId, e.getMessage());
                updateStatus(task, TaskStatus.PARTIAL_SUCCESS, 80,
                        "规则分析完成，AI 分析失败: " + e.getMessage());
            } catch (Exception e) {
                log.error("[Orchestrator] AI 分析发生意外错误, taskId={}: {}", taskId, e.getMessage(), e);
                updateStatus(task, TaskStatus.PARTIAL_SUCCESS, 80,
                        "规则分析完成，AI 分析异常: " + e.getMessage());
            }
        } else {
            // RULE_ONLY 类型
            updateStatus(task, TaskStatus.SUCCESS, 100, null);
            log.info("[Orchestrator] 仅规则分析完成，任务成功, taskId={}", taskId);
        }
    }

    // -------------------------------------------------------------------------

    private void updateStatus(AnalysisTask task, TaskStatus status, int progress, String errorMessage) {
        task.setStatus(status);
        task.setProgress(progress);
        task.setErrorMessage(errorMessage);
        analysisTaskRepository.save(task);
        log.info("[Orchestrator] 任务状态更新: taskId={}, status={}, progress={}",
                task.getId(), status, progress);
    }

    private void updateProgress(AnalysisTask task, int progress) {
        task.setProgress(progress);
        analysisTaskRepository.save(task);
    }
}
