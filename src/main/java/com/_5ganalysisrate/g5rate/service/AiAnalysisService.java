package com._5ganalysisrate.g5rate.service;

import com._5ganalysisrate.g5rate.domain.entity.AiAnalysisResult;
import com._5ganalysisrate.g5rate.domain.entity.RuleAnalysisResult;
import com._5ganalysisrate.g5rate.repository.AiAnalysisResultRepository;
import com._5ganalysisrate.g5rate.service.llm.LlmCallException;
import com._5ganalysisrate.g5rate.service.llm.LlmClient;
import com._5ganalysisrate.g5rate.service.llm.LlmResponse;
import com._5ganalysisrate.g5rate.service.prompt.PromptTemplateService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

/**
 * AI 分析服务
 * 负责构建提示词、调用 LLM、校验输出 JSON、持久化结果，支持最多 3 次重试
 */
@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);

    private static final int MAX_RETRY = 3;

    private final LlmClient llmClient;
    private final PromptTemplateService promptTemplateService;
    private final AiAnalysisResultRepository aiAnalysisResultRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.llm.model:gpt-4o-mini}")
    private String modelName;

    /**
     * 执行 AI 分析并持久化结果（含重试机制）
     *
     * @param taskId          任务 ID
     * @param ruleResult      规则分析结果（用于构建提示词上下文）
     * @return 保存后的 AI 分析结果实体
     * @throws AiAnalysisException 重试耗尽仍失败时抛出
     */
    @Transactional
    public AiAnalysisResult runAndPersist(Long taskId, RuleAnalysisResult ruleResult) {
        if (!llmClient.isAvailable()) {
            throw new AiAnalysisException("LLM 客户端未配置 API Key，无法执行 AI 分析。"
                    + "请在配置中设置 ai.llm.api-key");
        }

        String systemPrompt = promptTemplateService.buildSystemPrompt();
        String userPrompt = buildUserPrompt(ruleResult);
        String inputDigest = sha256Prefix(systemPrompt + userPrompt);

        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                log.info("[AiAnalysis] 第 {}/{} 次尝试, taskId={}", attempt, MAX_RETRY, taskId);

                LlmResponse response = llmClient.chat(systemPrompt, userPrompt);
                String outputJson = validateAndExtractJson(response.getContent());

                AiAnalysisResult result = buildEntity(taskId, response, outputJson, inputDigest);
                AiAnalysisResult saved = aiAnalysisResultRepository.save(result);
                log.info("[AiAnalysis] AI 分析成功, taskId={}, resultId={}, tokens={}, latency={}ms",
                        taskId, saved.getId(), response.getTotalTokens(), response.getLatencyMs());
                return saved;

            } catch (Exception e) {
                lastException = e;
                log.warn("[AiAnalysis] 第 {}/{} 次尝试失败, taskId={}, 原因: {}",
                        attempt, MAX_RETRY, taskId, e.getMessage());
                if (attempt < MAX_RETRY) {
                    sleepBeforeRetry(attempt);
                }
            }
        }

        throw new AiAnalysisException("AI 分析在 " + MAX_RETRY + " 次重试后仍失败: "
                + (lastException != null ? lastException.getMessage() : "未知错误"), lastException);
    }

    /**
     * 根据任务 ID 查询 AI 分析结果
     */
    public java.util.Optional<AiAnalysisResult> findByTaskId(Long taskId) {
        return aiAnalysisResultRepository.findByTaskId(taskId);
    }

    // -------------------------------------------------------------------------

    private String buildUserPrompt(RuleAnalysisResult ruleResult) {
        Map<String, Object> kpiSummary = parseJsonToMap(ruleResult.getKpiSummaryJson());
        String timeseriesInfo = summarizeTimeseries(ruleResult.getTimeseriesJson());
        String distributionInfo = summarizeDistribution(ruleResult.getDistributionJson());
        String peakRateInfo = summarizePeakRate(ruleResult.getPeakRateJson());
        return promptTemplateService.buildUserPrompt(kpiSummary, timeseriesInfo, distributionInfo, peakRateInfo, null);
    }

    /**
     * 校验 LLM 返回内容是否为合法 JSON，并包含必要字段
     */
    private String validateAndExtractJson(String content) {
        if (content == null || content.isBlank()) {
            throw new AiAnalysisException("LLM 返回内容为空");
        }

        // 尝试从 Markdown 代码块中提取 JSON
        String cleaned = content.trim();
        if (cleaned.startsWith("```")) {
            int start = cleaned.indexOf('\n') + 1;
            int end = cleaned.lastIndexOf("```");
            if (start > 0 && end > start) {
                cleaned = cleaned.substring(start, end).trim();
            }
        }

        try {
            JsonNode root = objectMapper.readTree(cleaned);

            // 检查必要字段
            if (!root.has("summary")) {
                throw new AiAnalysisException("AI 输出缺少必要字段: summary");
            }
            if (!root.has("findings")) {
                throw new AiAnalysisException("AI 输出缺少必要字段: findings");
            }
            if (!root.has("root_causes")) {
                throw new AiAnalysisException("AI 输出缺少必要字段: root_causes");
            }
            if (!root.has("actions")) {
                throw new AiAnalysisException("AI 输出缺少必要字段: actions");
            }
            if (!root.has("compliance_check")) {
                throw new AiAnalysisException("AI 输出缺少必要字段: compliance_check");
            }

            return cleaned;
        } catch (AiAnalysisException e) {
            throw e;
        } catch (Exception e) {
            throw new AiAnalysisException("AI 输出不是合法 JSON: " + e.getMessage());
        }
    }

    private AiAnalysisResult buildEntity(Long taskId, LlmResponse response,
                                          String outputJson, String inputDigest) {
        AiAnalysisResult entity = new AiAnalysisResult();
        entity.setTaskId(taskId);
        entity.setModelName(response.getModelName() != null ? response.getModelName() : modelName);
        entity.setPromptVersion(PromptTemplateService.CURRENT_VERSION);
        entity.setInputDigest(inputDigest);
        entity.setOutputJson(outputJson);
        entity.setTokenUsage(response.getTotalTokens());
        entity.setLatencyMs(response.getLatencyMs());
        // 置信度从 AI 输出中提取（取 root_causes 平均置信度）
        entity.setConfidenceScore(extractAvgConfidence(outputJson));
        return entity;
    }

    private double extractAvgConfidence(String outputJson) {
        try {
            JsonNode root = objectMapper.readTree(outputJson);
            JsonNode rootCauses = root.path("root_causes");
            if (rootCauses.isArray() && !rootCauses.isEmpty()) {
                double sum = 0;
                int count = 0;
                for (JsonNode rc : rootCauses) {
                    if (rc.has("confidence")) {
                        sum += rc.get("confidence").asDouble(0);
                        count++;
                    }
                }
                return count > 0 ? sum / count : 0.0;
            }
        } catch (Exception ignored) {
            // 忽略提取异常，返回默认值
        }
        return 0.0;
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

    private String summarizeTimeseries(String timeseriesJson) {
        if (timeseriesJson == null || timeseriesJson.isBlank()) return "无时序数据";
        try {
            JsonNode root = objectMapper.readTree(timeseriesJson);
            JsonNode series = root.path("series");
            if (series.isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode s : series) {
                    String name = s.path("name").asText();
                    int count = s.path("data").size();
                    sb.append("- ").append(name).append(": ").append(count).append(" 个数据点\n");
                }
                return sb.toString();
            }
        } catch (Exception e) {
            log.debug("[AiAnalysis] 时序数据摘要提取失败: {}", e.getMessage());
        }
        return "时序数据解析失败";
    }

    private String summarizeDistribution(String distributionJson) {
        if (distributionJson == null || distributionJson.isBlank()) return "无速率分布数据";
        try {
            JsonNode root = objectMapper.readTree(distributionJson);
            JsonNode pieData = root.path("pieData");
            if (pieData.isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode item : pieData) {
                    String name = item.path("name").asText();
                    double value = item.path("value").asDouble();
                    sb.append("- ").append(name).append(": ").append(value).append("%\n");
                }
                return sb.toString();
            }
        } catch (Exception e) {
            log.debug("[AiAnalysis] 分布数据摘要提取失败: {}", e.getMessage());
        }
        return "速率分布数据解析失败";
    }

    private String summarizePeakRate(String peakRateJson) {
        if (peakRateJson == null || peakRateJson.isBlank()) return "未计算峰值速率";
        try {
            JsonNode root = objectMapper.readTree(peakRateJson);
            StringBuilder sb = new StringBuilder();
            root.fields().forEachRemaining(e ->
                    sb.append("- ").append(e.getKey()).append(": ").append(e.getValue().asText()).append("\n"));
            return sb.toString();
        } catch (Exception e) {
            return "峰值速率数据解析失败";
        }
    }

    private String sha256Prefix(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (Exception e) {
            return "unknown";
        }
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            long delayMs = 1000L * attempt; // 递增延迟
            log.info("[AiAnalysis] 等待 {}ms 后重试...", delayMs);
            Thread.sleep(delayMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
