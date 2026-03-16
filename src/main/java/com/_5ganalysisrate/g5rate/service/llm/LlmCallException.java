package com._5ganalysisrate.g5rate.service.llm;

/**
 * LLM 调用异常
 */
public class LlmCallException extends RuntimeException {

    public LlmCallException(String message) {
        super(message);
    }

    public LlmCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
