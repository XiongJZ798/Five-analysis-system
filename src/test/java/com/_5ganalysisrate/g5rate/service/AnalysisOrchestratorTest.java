package com._5ganalysisrate.g5rate.service;

import com._5ganalysisrate.g5rate.domain.entity.AnalysisTask;
import com._5ganalysisrate.g5rate.domain.entity.RuleAnalysisResult;
import com._5ganalysisrate.g5rate.domain.enums.TaskStatus;
import com._5ganalysisrate.g5rate.domain.enums.TaskType;
import com._5ganalysisrate.g5rate.repository.AnalysisTaskRepository;
import com._5ganalysisrate.g5rate.service.orchestrator.AnalysisOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisOrchestratorTest {

    @Mock
    private AnalysisTaskRepository analysisTaskRepository;

    @Mock
    private RuleAnalysisService ruleAnalysisService;

    @Mock
    private AiAnalysisService aiAnalysisService;

    @InjectMocks
    private AnalysisOrchestrator orchestrator;

    private AnalysisTask mockTask;

    @BeforeEach
    void setUp() {
        mockTask = new AnalysisTask();
        mockTask.setId(1L);
        mockTask.setUploadFileId("test.xlsx");
        mockTask.setStatus(TaskStatus.PENDING);
        mockTask.setProgress(0);
    }

    @Test
    void testExecuteAsync_RuleOnly_Success() {
        mockTask.setTaskType(TaskType.RULE_ONLY);
        when(analysisTaskRepository.findById(1L)).thenReturn(Optional.of(mockTask));
        when(analysisTaskRepository.save(any())).thenReturn(mockTask);

        RuleAnalysisResult ruleResult = new RuleAnalysisResult();
        ruleResult.setId(10L);
        when(ruleAnalysisService.runAndPersist(1L)).thenReturn(ruleResult);

        orchestrator.executeAsync(1L);

        // 验证状态流转：PENDING -> RUNNING -> SUCCESS
        ArgumentCaptor<AnalysisTask> captor = ArgumentCaptor.forClass(AnalysisTask.class);
        verify(analysisTaskRepository, atLeastOnce()).save(captor.capture());

        AnalysisTask lastSaved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEquals(TaskStatus.SUCCESS, lastSaved.getStatus());
        assertEquals(100, lastSaved.getProgress());

        // RULE_ONLY 不应调用 AI 分析
        verify(aiAnalysisService, never()).runAndPersist(anyLong(), any());
    }

    @Test
    void testExecuteAsync_RuleAi_Success() {
        mockTask.setTaskType(TaskType.RULE_AI);
        when(analysisTaskRepository.findById(1L)).thenReturn(Optional.of(mockTask));
        when(analysisTaskRepository.save(any())).thenReturn(mockTask);

        RuleAnalysisResult ruleResult = new RuleAnalysisResult();
        ruleResult.setId(10L);
        when(ruleAnalysisService.runAndPersist(1L)).thenReturn(ruleResult);

        orchestrator.executeAsync(1L);

        // 验证 AI 分析被调用
        verify(aiAnalysisService, times(1)).runAndPersist(eq(1L), eq(ruleResult));

        // 验证最终状态为 SUCCESS
        ArgumentCaptor<AnalysisTask> captor = ArgumentCaptor.forClass(AnalysisTask.class);
        verify(analysisTaskRepository, atLeastOnce()).save(captor.capture());
        AnalysisTask lastSaved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEquals(TaskStatus.SUCCESS, lastSaved.getStatus());
    }

    @Test
    void testExecuteAsync_RuleFails_TaskFailed() {
        mockTask.setTaskType(TaskType.RULE_AI);
        when(analysisTaskRepository.findById(1L)).thenReturn(Optional.of(mockTask));
        when(analysisTaskRepository.save(any())).thenReturn(mockTask);
        when(ruleAnalysisService.runAndPersist(1L)).thenThrow(new RuntimeException("数据库连接失败"));

        orchestrator.executeAsync(1L);

        // 规则失败后，任务应为 FAILED
        ArgumentCaptor<AnalysisTask> captor = ArgumentCaptor.forClass(AnalysisTask.class);
        verify(analysisTaskRepository, atLeastOnce()).save(captor.capture());
        AnalysisTask lastSaved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEquals(TaskStatus.FAILED, lastSaved.getStatus());
        assertNotNull(lastSaved.getErrorMessage());

        // AI 分析不应被调用
        verify(aiAnalysisService, never()).runAndPersist(anyLong(), any());
    }

    @Test
    void testExecuteAsync_AiFails_PartialSuccess() {
        mockTask.setTaskType(TaskType.RULE_AI);
        when(analysisTaskRepository.findById(1L)).thenReturn(Optional.of(mockTask));
        when(analysisTaskRepository.save(any())).thenReturn(mockTask);

        RuleAnalysisResult ruleResult = new RuleAnalysisResult();
        ruleResult.setId(10L);
        when(ruleAnalysisService.runAndPersist(1L)).thenReturn(ruleResult);
        when(aiAnalysisService.runAndPersist(anyLong(), any()))
                .thenThrow(new AiAnalysisException("API Key 无效"));

        orchestrator.executeAsync(1L);

        // 规则成功但 AI 失败，状态应为 PARTIAL_SUCCESS
        ArgumentCaptor<AnalysisTask> captor = ArgumentCaptor.forClass(AnalysisTask.class);
        verify(analysisTaskRepository, atLeastOnce()).save(captor.capture());
        AnalysisTask lastSaved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEquals(TaskStatus.PARTIAL_SUCCESS, lastSaved.getStatus());
        assertNotNull(lastSaved.getErrorMessage());
    }

    @Test
    void testExecuteAsync_TaskNotFound() {
        when(analysisTaskRepository.findById(999L)).thenReturn(Optional.empty());

        // 不应抛出异常，只是记录日志并返回
        assertDoesNotThrow(() -> orchestrator.executeAsync(999L));

        // 不应调用任何分析服务
        verify(ruleAnalysisService, never()).runAndPersist(anyLong());
        verify(aiAnalysisService, never()).runAndPersist(anyLong(), any());
    }
}
