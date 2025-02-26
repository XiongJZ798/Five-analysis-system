package com._5ganalysisrate.g5rate.config;

import com._5ganalysisrate.g5rate.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResponse.error(500, "系统异常：" + e.getMessage());
    }

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("参数验证失败", e);
        return ApiResponse.error(400, "参数错误：" + e.getMessage());
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ApiResponse<Void> handleRuntimeException(RuntimeException e) {
        log.error("业务异常", e);
        return ApiResponse.error(500, "操作失败：" + e.getMessage());
    }
} 