package com._5ganalysisrate.g5rate.dto.ai;

import com._5ganalysisrate.g5rate.model.ai.AnalysisFeedback;
import lombok.Data;

@Data
public class FeedbackRequest {
    private String findingId;
    private AnalysisFeedback.FeedbackType feedbackType;
    private String comment;
    private String createdBy;
}
