package com._5ganalysisrate.g5rate.controller;

import com._5ganalysisrate.g5rate.config.SecurityConfig;
import com._5ganalysisrate.g5rate.config.WebConfig;
import com._5ganalysisrate.g5rate.domain.entity.AnalysisFeedback;
import com._5ganalysisrate.g5rate.domain.entity.AnalysisTask;
import com._5ganalysisrate.g5rate.domain.enums.TaskStatus;
import com._5ganalysisrate.g5rate.domain.enums.TaskType;
import com._5ganalysisrate.g5rate.dto.*;
import com._5ganalysisrate.g5rate.service.AnalysisTaskService;
import com._5ganalysisrate.g5rate.service.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalysisTaskController.class)
@Import({SecurityConfig.class, WebConfig.class})
class AnalysisTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalysisTaskService analysisTaskService;

    @MockBean
    private ReportService reportService;

    @Autowired
    private ObjectMapper objectMapper;

    private AnalysisTask sampleTask;
    private AnalysisTaskResponse sampleTaskResponse;

    @BeforeEach
    void setUp() {
        sampleTask = new AnalysisTask();
        sampleTask.setId(1L);
        sampleTask.setUploadFileId("test.xlsx");
        sampleTask.setTaskType(TaskType.RULE_AI);
        sampleTask.setStatus(TaskStatus.PENDING);
        sampleTask.setProgress(0);
        sampleTask.setCreatedAt(LocalDateTime.now());
        sampleTask.setUpdatedAt(LocalDateTime.now());

        sampleTaskResponse = new AnalysisTaskResponse();
        sampleTaskResponse.setTaskId(1L);
        sampleTaskResponse.setUploadFileId("test.xlsx");
        sampleTaskResponse.setTaskType(TaskType.RULE_AI);
        sampleTaskResponse.setStatus(TaskStatus.PENDING);
        sampleTaskResponse.setProgress(0);
        sampleTaskResponse.setCreatedAt(LocalDateTime.now());
        sampleTaskResponse.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void testCreateTask_Success() throws Exception {
        when(analysisTaskService.createTask(any())).thenReturn(sampleTask);
        when(reportService.buildTaskResponse(sampleTask)).thenReturn(sampleTaskResponse);

        CreateAnalysisTaskRequest request = new CreateAnalysisTaskRequest();
        request.setFileId("test.xlsx");
        request.setTaskType(TaskType.RULE_AI);

        mockMvc.perform(post("/analysis/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskId").value(1))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void testCreateTask_MissingFileId() throws Exception {
        CreateAnalysisTaskRequest request = new CreateAnalysisTaskRequest();
        request.setFileId("");

        mockMvc.perform(post("/analysis/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void testGetTask_Success() throws Exception {
        when(analysisTaskService.findById(1L)).thenReturn(Optional.of(sampleTask));
        when(reportService.buildTaskResponse(sampleTask)).thenReturn(sampleTaskResponse);

        mockMvc.perform(get("/analysis/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskId").value(1));
    }

    @Test
    void testGetTask_NotFound() throws Exception {
        when(analysisTaskService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/analysis/tasks/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void testGetRuleResult_Success() throws Exception {
        when(analysisTaskService.findById(1L)).thenReturn(Optional.of(sampleTask));
        RuleAnalysisResultResponse ruleResponse = new RuleAnalysisResultResponse();
        ruleResponse.setResultId(10L);
        ruleResponse.setTaskId(1L);
        when(reportService.buildRuleResultResponse(1L)).thenReturn(Optional.of(ruleResponse));

        mockMvc.perform(get("/analysis/tasks/1/rule-result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.resultId").value(10));
    }

    @Test
    void testGetRuleResult_NotFound() throws Exception {
        when(analysisTaskService.findById(1L)).thenReturn(Optional.of(sampleTask));
        when(reportService.buildRuleResultResponse(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/analysis/tasks/1/rule-result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void testGetAiResult_Success() throws Exception {
        when(analysisTaskService.findById(1L)).thenReturn(Optional.of(sampleTask));
        AiAnalysisResultResponse aiResponse = new AiAnalysisResultResponse();
        aiResponse.setResultId(20L);
        aiResponse.setTaskId(1L);
        aiResponse.setSummary("性能分析正常");
        when(reportService.buildAiResultResponse(1L)).thenReturn(Optional.of(aiResponse));

        mockMvc.perform(get("/analysis/tasks/1/ai-result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.summary").value("性能分析正常"));
    }

    @Test
    void testGetReport_Success() throws Exception {
        when(analysisTaskService.findById(1L)).thenReturn(Optional.of(sampleTask));
        AnalysisReportResponse report = new AnalysisReportResponse();
        report.setTaskId(1L);
        report.setStatus(TaskStatus.SUCCESS);
        report.setGeneratedAt(LocalDateTime.now());
        when(reportService.buildReport(sampleTask)).thenReturn(report);

        mockMvc.perform(get("/analysis/tasks/1/report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskId").value(1));
    }

    @Test
    void testSubmitFeedback_Success() throws Exception {
        AnalysisFeedback feedback = new AnalysisFeedback();
        feedback.setId(5L);
        feedback.setTaskId(1L);
        when(analysisTaskService.submitFeedback(eq(1L), any())).thenReturn(feedback);

        CreateFeedbackRequest request = new CreateFeedbackRequest();
        request.setFindingId("F1");
        request.setFeedbackType("ACCEPT");
        request.setComment("建议已采纳");

        mockMvc.perform(post("/analysis/tasks/1/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(5));
    }

    @Test
    void testSubmitFeedback_InvalidType() throws Exception {
        when(analysisTaskService.submitFeedback(eq(1L), any()))
                .thenThrow(new IllegalArgumentException("feedbackType 必须为 ACCEPT / REJECT / PARTIAL"));

        CreateFeedbackRequest request = new CreateFeedbackRequest();
        request.setFeedbackType("INVALID");

        mockMvc.perform(post("/analysis/tasks/1/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}
