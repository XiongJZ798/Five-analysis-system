package com._5ganalysisrate.g5rate.service.llm;

import lombok.Data;

/**
 * LLM 调用响应
 */
@Data
public class LlmResponse {

    /** 模型返回的原始文本内容 */
    private String content;

    /** 消耗的 Token 数量（prompt + completion） */
    private int totalTokens;

    /** 实际调用耗时（ms） */
    private long latencyMs;

    /** 使用的模型名称 */
    private String modelName;

    public LlmResponse(String content, int totalTokens, long latencyMs, String modelName) {
        this.content = content;
        this.totalTokens = totalTokens;
        this.latencyMs = latencyMs;
        this.modelName = modelName;
    }
}
