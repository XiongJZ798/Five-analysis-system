package com._5ganalysisrate.g5rate.service;

import com._5ganalysisrate.g5rate.domain.entity.AiAnalysisResult;
import com._5ganalysisrate.g5rate.domain.entity.RuleAnalysisResult;
import com._5ganalysisrate.g5rate.repository.AiAnalysisResultRepository;
import com._5ganalysisrate.g5rate.service.llm.LlmClient;
import com._5ganalysisrate.g5rate.service.llm.LlmResponse;
import com._5ganalysisrate.g5rate.service.prompt.PromptTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiAnalysisServiceTest {

    @Mock
    private LlmClient llmClient;

    @Spy
    private PromptTemplateService promptTemplateService;

    @Mock
    private AiAnalysisResultRepository aiAnalysisResultRepository;

    @InjectMocks
    private AiAnalysisService aiAnalysisService;

    private static final String VALID_AI_OUTPUT = """
            {
              "summary": "5G 网络整体性能良好，存在轻微下行吞吐量波动",
              "findings": [
                {
                  "id": "F1",
                  "title": "下行吞吐量周期性波动",
                  "description": "MAC 层下行速率存在周期性抖动",
                  "evidence": [{"metric": "DL_throughput_p50", "value": 85.2, "unit": "Mbps"}],
                  "severity": "MEDIUM"
                }
              ],
              "root_causes": [
                {"finding_id": "F1", "cause": "PRB 占用率过高导致调度压力", "confidence": 0.75}
              ],
              "actions": [
                {
                  "priority": "P1",
                  "action": "调整 CQI 反馈周期",
                  "expected_gain": "提升 5-10% 下行吞吐量",
                  "risk": "低，不影响现网稳定性"
                }
              ],
              "compliance_check": {"is_3gpp_aligned": true, "notes": "符合 TS 38.214"},
              "limitations": ["数据样本量有限，分析结论仅供参考"]
            }
            """;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiAnalysisService, "modelName", "gpt-4o-mini");
    }

    @Test
    void testRunAndPersist_Success() {
        // 准备
        when(llmClient.isAvailable()).thenReturn(true);
        LlmResponse mockResponse = new LlmResponse(VALID_AI_OUTPUT, 500, 1200, "gpt-4o-mini");
        when(llmClient.chat(anyString(), anyString())).thenReturn(mockResponse);

        AiAnalysisResult savedEntity = new AiAnalysisResult();
        savedEntity.setId(1L);
        savedEntity.setTaskId(10L);
        savedEntity.setModelName("gpt-4o-mini");
        when(aiAnalysisResultRepository.save(any())).thenReturn(savedEntity);

        RuleAnalysisResult ruleResult = new RuleAnalysisResult();
        ruleResult.setTaskId(10L);
        ruleResult.setKpiSummaryJson("{\"total_records\":100}");
        ruleResult.setTimeseriesJson("{\"series\":[]}");
        ruleResult.setDistributionJson("{\"pieData\":[]}");
        ruleResult.setPeakRateJson("{\"FDD_100MHz\":3000.0}");

        // 执行
        AiAnalysisResult result = aiAnalysisService.runAndPersist(10L, ruleResult);

        // 验证
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(llmClient, times(1)).chat(anyString(), anyString());
        verify(aiAnalysisResultRepository, times(1)).save(any());
    }

    @Test
    void testRunAndPersist_LlmNotAvailable() {
        when(llmClient.isAvailable()).thenReturn(false);

        RuleAnalysisResult ruleResult = new RuleAnalysisResult();
        ruleResult.setTaskId(10L);

        assertThrows(AiAnalysisException.class,
                () -> aiAnalysisService.runAndPersist(10L, ruleResult));
    }

    @Test
    void testRunAndPersist_InvalidJson_Retries() {
        when(llmClient.isAvailable()).thenReturn(true);
        // LLM 返回无效 JSON（模拟所有 3 次都失败）
        when(llmClient.chat(anyString(), anyString()))
                .thenReturn(new LlmResponse("这不是一个有效的JSON", 50, 100, "gpt-4o-mini"));

        RuleAnalysisResult ruleResult = new RuleAnalysisResult();
        ruleResult.setTaskId(10L);

        // 应在 3 次重试后抛出异常
        assertThrows(AiAnalysisException.class,
                () -> aiAnalysisService.runAndPersist(10L, ruleResult));

        // 应调用 3 次
        verify(llmClient, times(3)).chat(anyString(), anyString());
    }

    @Test
    void testRunAndPersist_MissingRequiredField() {
        when(llmClient.isAvailable()).thenReturn(true);
        // 缺少 "findings" 字段
        String incompleteJson = "{\"summary\":\"ok\",\"root_causes\":[],\"actions\":[],\"compliance_check\":{}}";
        when(llmClient.chat(anyString(), anyString()))
                .thenReturn(new LlmResponse(incompleteJson, 100, 200, "gpt-4o-mini"));

        RuleAnalysisResult ruleResult = new RuleAnalysisResult();
        ruleResult.setTaskId(10L);

        assertThrows(AiAnalysisException.class,
                () -> aiAnalysisService.runAndPersist(10L, ruleResult));
    }

    @Test
    void testFindByTaskId() {
        AiAnalysisResult entity = new AiAnalysisResult();
        entity.setId(5L);
        entity.setTaskId(10L);
        when(aiAnalysisResultRepository.findByTaskId(10L)).thenReturn(Optional.of(entity));

        Optional<AiAnalysisResult> result = aiAnalysisService.findByTaskId(10L);

        assertTrue(result.isPresent());
        assertEquals(5L, result.get().getId());
    }

    @Test
    void testRunAndPersist_MarkdownWrappedJson() {
        when(llmClient.isAvailable()).thenReturn(true);
        String markdownWrapped = "```json\n" + VALID_AI_OUTPUT + "\n```";
        when(llmClient.chat(anyString(), anyString()))
                .thenReturn(new LlmResponse(markdownWrapped, 500, 1200, "gpt-4o-mini"));

        AiAnalysisResult savedEntity = new AiAnalysisResult();
        savedEntity.setId(2L);
        savedEntity.setTaskId(10L);
        when(aiAnalysisResultRepository.save(any())).thenReturn(savedEntity);

        RuleAnalysisResult ruleResult = new RuleAnalysisResult();
        ruleResult.setTaskId(10L);

        AiAnalysisResult result = aiAnalysisService.runAndPersist(10L, ruleResult);

        assertNotNull(result);
    }
}
