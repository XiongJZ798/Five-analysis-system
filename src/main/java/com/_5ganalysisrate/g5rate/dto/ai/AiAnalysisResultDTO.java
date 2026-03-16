package com._5ganalysisrate.g5rate.dto.ai;

import lombok.Data;
import java.util.List;

/**
 * AI分析结果DTO，对应AI输出的标准JSON结构
 */
@Data
public class AiAnalysisResultDTO {

    private String summary;
    private List<Finding> findings;
    private List<RootCause> rootCauses;
    private List<Action> actions;
    private ComplianceCheck complianceCheck;
    private List<String> limitations;

    @Data
    public static class Finding {
        private String id;
        private String title;
        private String description;
        private List<Evidence> evidence;
        private String severity;
    }

    @Data
    public static class Evidence {
        private String metric;
        private Double value;
        private String unit;
    }

    @Data
    public static class RootCause {
        private String findingId;
        private String cause;
        private Double confidence;
    }

    @Data
    public static class Action {
        private String priority;
        private String action;
        private String expectedGain;
        private String risk;
    }

    @Data
    public static class ComplianceCheck {
        private boolean is3gppAligned;
        private String notes;
    }
}
