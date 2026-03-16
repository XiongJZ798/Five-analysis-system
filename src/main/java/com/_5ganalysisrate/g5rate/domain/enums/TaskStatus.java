package com._5ganalysisrate.g5rate.domain.enums;

/**
 * 分析任务状态
 */
public enum TaskStatus {
    /** 待处理 */
    PENDING,
    /** 处理中 */
    RUNNING,
    /** 全部成功 */
    SUCCESS,
    /** 规则成功，AI失败 */
    PARTIAL_SUCCESS,
    /** 失败 */
    FAILED
}
