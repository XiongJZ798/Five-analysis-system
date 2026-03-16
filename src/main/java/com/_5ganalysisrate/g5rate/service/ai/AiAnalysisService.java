package com._5ganalysisrate.g5rate.service.ai;

import com._5ganalysisrate.g5rate.dto.ai.AiAnalysisResultDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * AI大模型分析服务
 * 负责组装Prompt、调用AI接口、解析和校验输出JSON
 */
@Slf4j
@Service
public class AiAnalysisService {

    private static final String PROMPT_VERSION = "v1.0";

    @Value("${ai.api.url:}")
    private String aiApiUrl;

    @Value("${ai.api.key:}")
    private String aiApiKey;

    @Value("${ai.api.model:gpt-3.5-turbo}")
    private String aiModel;

    @Value("${ai.api.timeout-seconds:60}")
    private int timeoutSeconds;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * 对规则分析结果调用AI进行诊断分析
     *
     * @param fileName       上传的文件名
     * @param kpiSummary     KPI汇总数据
     * @param ruleResults    规则分析结果（时序、分布、峰值速率）
     * @return AI分析结果DTO
     */
    public AiAnalysisResultDTO analyze(String fileName, Map<String, Object> kpiSummary,
                                        Map<String, Object> ruleResults) {
        long start = System.currentTimeMillis();
        log.info("开始AI分析, 文件: {}", fileName);

        try {
            String inputDigest = computeDigest(fileName, kpiSummary);
            String prompt = buildPrompt(fileName, kpiSummary, ruleResults);

            String rawJson;
            if (aiApiUrl == null || aiApiUrl.isBlank()) {
                log.warn("AI API URL未配置，使用模拟分析结果");
                rawJson = buildMockResult(fileName, kpiSummary);
            } else {
                rawJson = callAiApi(prompt);
            }

            AiAnalysisResultDTO result = parseAndValidate(rawJson);
            long latency = System.currentTimeMillis() - start;
            log.info("AI分析完成, 文件: {}, 耗时: {}ms", fileName, latency);
            return result;
        } catch (Exception e) {
            log.error("AI分析失败, 文件: {}", fileName, e);
            throw new RuntimeException("AI分析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 计算输入数据摘要，用于去重和审计
     */
    public String computeDigest(String fileName, Map<String, Object> kpiSummary) {
        try {
            String input = fileName + objectMapper.writeValueAsString(kpiSummary);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String buildPrompt(String fileName, Map<String, Object> kpiSummary,
                                Map<String, Object> ruleResults) {
        try {
            String kpiJson = objectMapper.writeValueAsString(kpiSummary);
            String ruleJson = objectMapper.writeValueAsString(ruleResults);
            return String.format("""
                    你是一位5G网络性能优化专家，请基于以下测试数据分析结果进行诊断和优化建议。
                    
                    文件名: %s
                    
                    KPI汇总:
                    %s
                    
                    规则分析结果:
                    %s
                    
                    请严格按照以下JSON格式输出分析结果，不要输出任何其他内容：
                    {
                      "summary": "总体诊断摘要",
                      "findings": [
                        {
                          "id": "F1",
                          "title": "问题标题",
                          "description": "详细描述",
                          "evidence": [{"metric": "指标名", "value": 0.0, "unit": "单位"}],
                          "severity": "HIGH|MEDIUM|LOW"
                        }
                      ],
                      "root_causes": [
                        {
                          "finding_id": "F1",
                          "cause": "可能根因",
                          "confidence": 0.8
                        }
                      ],
                      "actions": [
                        {
                          "priority": "P0|P1|P2",
                          "action": "优化动作",
                          "expected_gain": "预期收益",
                          "risk": "风险说明"
                        }
                      ],
                      "compliance_check": {
                        "is_3gpp_aligned": true,
                        "notes": "合规说明"
                      },
                      "limitations": ["分析局限性说明"]
                    }
                    """, fileName, kpiJson, ruleJson);
        } catch (Exception e) {
            return "分析以下5G网络数据: " + fileName;
        }
    }

    private String callAiApi(String prompt) throws Exception {
        Map<String, Object> requestBody = Map.of(
                "model", aiModel,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3,
                "max_tokens", 2000
        );

        String body = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(aiApiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiApiKey)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("AI API调用失败, HTTP状态码: " + response.statusCode());
        }

        // 解析OpenAI格式响应，提取content字段
        Map<?, ?> responseMap = objectMapper.readValue(response.body(), Map.class);
        List<?> choices = (List<?>) responseMap.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("AI API返回结果为空");
        }
        Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
        Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
        return (String) message.get("content");
    }

    /**
     * 解析并校验AI输出JSON，确保符合规范结构
     */
    private AiAnalysisResultDTO parseAndValidate(String rawJson) {
        try {
            // 尝试直接解析
            AiAnalysisResultDTO result = objectMapper.readValue(rawJson, AiAnalysisResultDTO.class);
            validateResult(result);
            return result;
        } catch (Exception e) {
            log.warn("AI输出JSON解析失败，尝试提取JSON块: {}", e.getMessage());
            // 尝试从文本中提取JSON块
            String extracted = extractJsonBlock(rawJson);
            try {
                AiAnalysisResultDTO result = objectMapper.readValue(extracted, AiAnalysisResultDTO.class);
                validateResult(result);
                return result;
            } catch (Exception ex) {
                throw new RuntimeException("AI输出JSON格式不合规: " + ex.getMessage(), ex);
            }
        }
    }

    private void validateResult(AiAnalysisResultDTO result) {
        if (result.getSummary() == null || result.getSummary().isBlank()) {
            throw new IllegalArgumentException("AI输出缺少summary字段");
        }
        if (result.getFindings() == null) {
            throw new IllegalArgumentException("AI输出缺少findings字段");
        }
    }

    private String extractJsonBlock(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        throw new RuntimeException("无法从AI输出中提取JSON");
    }

    /**
     * 生成模拟的AI分析结果（用于未配置AI API时）
     */
    private String buildMockResult(String fileName, Map<String, Object> kpiSummary) {
        try {
            double avgThroughput = 0;
            double avgRsrp = 0;
            double avgSinr = 0;
            if (kpiSummary.containsKey("avgMacThroughput")) {
                avgThroughput = ((Number) kpiSummary.get("avgMacThroughput")).doubleValue();
            }
            if (kpiSummary.containsKey("avgRsrp")) {
                avgRsrp = ((Number) kpiSummary.get("avgRsrp")).doubleValue();
            }
            if (kpiSummary.containsKey("avgSinr")) {
                avgSinr = ((Number) kpiSummary.get("avgSinr")).doubleValue();
            }

            String severity = avgThroughput < 50 ? "HIGH" : avgThroughput < 100 ? "MEDIUM" : "LOW";
            boolean is3gppAligned = avgRsrp > -110 && avgSinr > 0;

            AiAnalysisResultDTO result = new AiAnalysisResultDTO();
            result.setSummary(String.format(
                    "文件[%s]的5G网络性能分析：平均下行速率%.1fMbps，RSRP均值%.1fdBm，SINR均值%.1fdB。%s",
                    fileName, avgThroughput, avgRsrp, avgSinr,
                    avgThroughput < 100 ? "网络性能存在优化空间。" : "网络性能基本正常。"
            ));

            AiAnalysisResultDTO.Finding finding = new AiAnalysisResultDTO.Finding();
            finding.setId("F1");
            finding.setTitle("下行吞吐量分析");
            finding.setDescription(String.format("平均MAC下行速率为%.1fMbps", avgThroughput));
            AiAnalysisResultDTO.Evidence evidence = new AiAnalysisResultDTO.Evidence();
            evidence.setMetric("DL_throughput_avg");
            evidence.setValue(avgThroughput);
            evidence.setUnit("Mbps");
            finding.setEvidence(List.of(evidence));
            finding.setSeverity(severity);
            result.setFindings(List.of(finding));

            AiAnalysisResultDTO.RootCause rootCause = new AiAnalysisResultDTO.RootCause();
            rootCause.setFindingId("F1");
            rootCause.setCause(avgRsrp < -100 ? "信号覆盖不足，建议检查基站覆盖范围" : "网络负载正常，可进一步优化参数配置");
            rootCause.setConfidence(0.75);
            result.setRootCauses(List.of(rootCause));

            AiAnalysisResultDTO.Action action = new AiAnalysisResultDTO.Action();
            action.setPriority(avgThroughput < 50 ? "P0" : "P1");
            action.setAction(avgRsrp < -100 ? "优化基站天线方向角和下倾角" : "调整MCS自适应算法参数");
            action.setExpectedGain("预计提升下行速率10-20%");
            action.setRisk("调整参数可能短暂影响在线用户体验");
            result.setActions(List.of(action));

            AiAnalysisResultDTO.ComplianceCheck compliance = new AiAnalysisResultDTO.ComplianceCheck();
            compliance.set3gppAligned(is3gppAligned);
            compliance.setNotes(is3gppAligned ? "符合3GPP NR规范要求" : "部分指标偏离3GPP NR规范建议值");
            result.setComplianceCheck(compliance);

            result.setLimitations(List.of(
                    "本分析基于上传数据的统计特征，未考虑跨批次时间维度对比",
                    "AI模型为模拟模式，实际部署需配置真实AI API"
            ));

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            throw new RuntimeException("生成模拟结果失败", e);
        }
    }
}
