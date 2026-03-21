package com._5ganalysisrate.g5rate.service.prompt;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 提示词模板服务
 * 维护版本化的中文 5G 优化分析提示词
 */
@Service
public class PromptTemplateService {

    /** 当前提示词版本 */
    public static final String CURRENT_VERSION = "v1.0";

    /**
     * 获取当前系统提示词版本
     */
    public String getCurrentVersion() {
        return CURRENT_VERSION;
    }

    /**
     * 构建系统提示词（角色、约束、输出格式）
     */
    public String buildSystemPrompt() {
        return """
                你是一名专业的 5G 无线网络性能优化工程师，具备深厚的 3GPP 标准知识和丰富的网络优化经验。
                
                【职责】
                - 基于系统提供的 KPI 指标和规则分析结果，对 5G 网络性能进行专业诊断
                - 识别网络问题，推断可能的根因，并给出具体可落地的优化建议
                
                【约束】
                1. 所有结论必须基于输入的指标数据，不允许编造不存在的数据
                2. 必须遵循 3GPP 标准规范（TS 38.306、TS 38.214 等），不能违背技术原理
                3. 根因分析必须标注置信度（0.0~1.0），不确定的信息置信度应低于 0.5
                4. 优化建议必须注明预期收益和潜在风险
                5. 若数据不足以支撑某项结论，必须在 limitations 中说明
                6. 严禁捏造指标值或夸大分析结论
                
                【输出格式】
                必须严格按照以下 JSON Schema 输出，不得包含任何 JSON 之外的内容：
                {
                  "summary": "string",
                  "findings": [
                    {
                      "id": "F1",
                      "title": "string",
                      "description": "string",
                      "evidence": [{"metric": "string", "value": 0.0, "unit": "string"}],
                      "severity": "HIGH|MEDIUM|LOW"
                    }
                  ],
                  "root_causes": [
                    {"finding_id": "F1", "cause": "string", "confidence": 0.0}
                  ],
                  "actions": [
                    {
                      "priority": "P0|P1|P2",
                      "action": "string",
                      "expected_gain": "string",
                      "risk": "string"
                    }
                  ],
                  "compliance_check": {
                    "is_3gpp_aligned": true,
                    "notes": "string"
                  },
                  "limitations": ["string"]
                }
                """;
    }

    /**
     * 构建用户提示词，注入规则分析结果和 KPI 摘要
     *
     * @param kpiSummary    KPI 统计摘要
     * @param timeseriesInfo 时序异常摘要
     * @param distributionInfo 速率分布摘要
     * @param peakRateInfo 峰值速率计算结果
     * @param extraContext  额外上下文（可为 null）
     */
    public String buildUserPrompt(Map<String, Object> kpiSummary,
                                   String timeseriesInfo,
                                   String distributionInfo,
                                   String peakRateInfo,
                                   String extraContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下是本次 5G 网络性能测试数据的分析结果，请根据这些数据给出专业诊断报告。\n\n");

        sb.append("## KPI 统计摘要\n");
        if (kpiSummary != null && !kpiSummary.isEmpty()) {
            kpiSummary.forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append("\n"));
        } else {
            sb.append("- 暂无 KPI 统计数据\n");
        }

        sb.append("\n## 时序分析摘要\n");
        sb.append(timeseriesInfo != null ? timeseriesInfo : "暂无时序分析数据");

        sb.append("\n\n## 速率分布摘要\n");
        sb.append(distributionInfo != null ? distributionInfo : "暂无速率分布数据");

        sb.append("\n\n## 3GPP 峰值速率计算结果\n");
        sb.append(peakRateInfo != null ? peakRateInfo : "未执行峰值速率计算");

        if (extraContext != null && !extraContext.isBlank()) {
            sb.append("\n\n## 附加背景信息\n").append(extraContext);
        }

        sb.append("\n\n请根据以上数据，按照要求的 JSON 格式输出分析报告。");

        return sb.toString();
    }
}
