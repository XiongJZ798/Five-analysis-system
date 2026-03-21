package com._5ganalysisrate.g5rate.service;

import com._5ganalysisrate.g5rate.domain.entity.AnalysisFeedback;
import com._5ganalysisrate.g5rate.domain.entity.AnalysisTask;
import com._5ganalysisrate.g5rate.domain.enums.TaskStatus;
import com._5ganalysisrate.g5rate.domain.enums.TaskType;
import com._5ganalysisrate.g5rate.dto.CreateAnalysisTaskRequest;
import com._5ganalysisrate.g5rate.dto.CreateFeedbackRequest;
import com._5ganalysisrate.g5rate.repository.AnalysisFeedbackRepository;
import com._5ganalysisrate.g5rate.repository.AnalysisTaskRepository;
import com._5ganalysisrate.g5rate.service.orchestrator.AnalysisOrchestrator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 分析任务 CRUD 与生命周期服务
 */
@Service
@RequiredArgsConstructor
public class AnalysisTaskService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisTaskService.class);

    private final AnalysisTaskRepository analysisTaskRepository;
    private final AnalysisFeedbackRepository analysisFeedbackRepository;
    private final AnalysisOrchestrator analysisOrchestrator;

    /**
     * 创建新分析任务并异步触发执行
     */
    @Transactional
    public AnalysisTask createTask(CreateAnalysisTaskRequest request) {
        if (request.getFileId() == null || request.getFileId().isBlank()) {
            throw new IllegalArgumentException("fileId 不能为空");
        }

        AnalysisTask task = new AnalysisTask();
        task.setUploadFileId(request.getFileId());
        task.setTaskType(request.getTaskType() != null ? request.getTaskType() : TaskType.RULE_AI);
        task.setStatus(TaskStatus.PENDING);
        task.setProgress(0);

        AnalysisTask saved = analysisTaskRepository.save(task);
        log.info("[TaskService] 任务已创建, taskId={}, type={}", saved.getId(), saved.getTaskType());

        // 异步触发分析流水线
        analysisOrchestrator.executeAsync(saved.getId());

        return saved;
    }

    /**
     * 根据 taskId 查询任务
     */
    public Optional<AnalysisTask> findById(Long taskId) {
        return analysisTaskRepository.findById(taskId);
    }

    /**
     * 提交工程师反馈
     */
    @Transactional
    public AnalysisFeedback submitFeedback(Long taskId, CreateFeedbackRequest request) {
        // 验证任务存在
        analysisTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + taskId));

        // 验证反馈类型
        String feedbackType = request.getFeedbackType();
        if (feedbackType == null || feedbackType.isBlank()) {
            throw new IllegalArgumentException("feedbackType 不能为空");
        }
        if (!feedbackType.equals("ACCEPT") && !feedbackType.equals("REJECT") && !feedbackType.equals("PARTIAL")) {
            throw new IllegalArgumentException("feedbackType 必须为 ACCEPT / REJECT / PARTIAL");
        }

        AnalysisFeedback feedback = new AnalysisFeedback();
        feedback.setTaskId(taskId);
        feedback.setFindingId(request.getFindingId());
        feedback.setFeedbackType(feedbackType);
        feedback.setComment(request.getComment());
        feedback.setCreatedBy(request.getCreatedBy());

        AnalysisFeedback saved = analysisFeedbackRepository.save(feedback);
        log.info("[TaskService] 反馈已提交, taskId={}, feedbackId={}", taskId, saved.getId());
        return saved;
    }
}
