package com._5ganalysisrate.g5rate.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容 LLM 客户端实现
 * 支持 OpenAI / Azure OpenAI / 通义千问 / DeepSeek / 其他兼容接口
 */
@Component
public class OpenAiCompatibleLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmClient.class);

    @Value("${ai.llm.api-url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${ai.llm.api-key:}")
    private String apiKey;

    @Value("${ai.llm.model:gpt-4o-mini}")
    private String model;

    @Value("${ai.llm.timeout-seconds:120}")
    private int timeoutSeconds;

    @Value("${ai.llm.max-tokens:4096}")
    private int maxTokens;

    @Value("${ai.llm.temperature:0.2}")
    private double temperature;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public LlmResponse chat(String systemPrompt, String userPrompt) {
        long startTime = System.currentTimeMillis();

        try {
            String requestBody;
            try {
                requestBody = buildRequestBody(systemPrompt, userPrompt);
            } catch (Exception e) {
                throw new LlmCallException("构建请求体失败: " + e.getMessage(), e);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LlmCallException("LLM API 网络请求异常: " + e.getMessage(), e);
            }

            long latency = System.currentTimeMillis() - startTime;

            if (response.statusCode() != 200) {
                throw new LlmCallException("LLM API 返回非200状态码: " + response.statusCode()
                        + " body=" + response.body());
            }

            try {
                return parseResponse(response.body(), latency);
            } catch (LlmCallException e) {
                throw e;
            } catch (Exception e) {
                throw new LlmCallException("解析 LLM 响应失败: " + e.getMessage(), e);
            }

        } catch (LlmCallException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmCallException("LLM API 调用异常: " + e.getMessage(), e);
        }
    }

    private String buildRequestBody(String systemPrompt, String userPrompt) throws Exception {
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", temperature,
                "max_tokens", maxTokens,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "response_format", Map.of("type", "json_object")
        );
        return objectMapper.writeValueAsString(body);
    }

    private LlmResponse parseResponse(String responseBody, long latencyMs) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new LlmCallException("LLM 响应缺少 choices 字段或为空");
        }
        String content = choices.get(0).path("message").path("content").asText();
        if (content.isBlank()) {
            throw new LlmCallException("LLM 响应 content 为空");
        }
        int totalTokens = root.path("usage").path("total_tokens").asInt(0);
        String usedModel = root.path("model").asText(model);

        log.info("[LLM] model={} tokens={} latency={}ms", usedModel, totalTokens, latencyMs);

        return new LlmResponse(content, totalTokens, latencyMs, usedModel);
    }
}
