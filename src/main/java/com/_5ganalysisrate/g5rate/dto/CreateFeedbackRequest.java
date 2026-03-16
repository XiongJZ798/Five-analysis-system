package com._5ganalysisrate.g5rate.dto;

import lombok.Data;

/**
 * 创建工程师反馈请求 DTO
 */
@Data
public class CreateFeedbackRequest {

    /** 关联发现 ID，如 F1 */
    private String findingId;

    /**
     * 反馈类型: ACCEPT / REJECT / PARTIAL
     */
    private String feedbackType;

    /** 备注信息 */
    private String comment;

    /** 反馈人 */
    private String createdBy;
}
