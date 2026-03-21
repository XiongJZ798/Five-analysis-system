package com._5ganalysisrate.g5rate.service.llm;

/**
 * LLM 客户端抽象接口
 * 支持任意 OpenAI 兼容 API
 */
public interface LlmClient {

    /**
     * 同步调用 LLM，返回纯文本响应
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @return LLM 返回的文本内容
     */
    LlmResponse chat(String systemPrompt, String userPrompt);

    /**
     * 判断客户端是否可用（配置了 API key 等必要参数）
     */
    boolean isAvailable();
}
