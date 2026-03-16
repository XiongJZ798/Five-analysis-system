package com._5ganalysisrate.g5rate.service;

import com._5ganalysisrate.g5rate.domain.entity.RuleAnalysisResult;
import com._5ganalysisrate.g5rate.repository.RuleAnalysisResultRepository;
import com._5ganalysisrate.g5rate.repository.TestDataRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 规则分析服务
 * 封装现有的 AnalysisService，整合时序、分布、峰值速率分析，持久化结果
 */
@Service
@RequiredArgsConstructor
public class RuleAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(RuleAnalysisService.class);

    private final AnalysisService analysisService;
    private final TestDataRepository testDataRepository;
    private final RuleAnalysisResultRepository ruleAnalysisResultRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 执行规则分析并持久化结果
     *
     * @param taskId 任务ID
     * @return 保存后的规则分析结果实体
     */
    @Transactional
    public RuleAnalysisResult runAndPersist(Long taskId) {
        log.info("[RuleAnalysis] 开始规则分析, taskId={}", taskId);

        RuleAnalysisResult result = new RuleAnalysisResult();
        result.setTaskId(taskId);

        // 1. KPI 统计汇总
        Map<String, Object> kpiSummary = buildKpiSummary();
        result.setKpiSummaryJson(toJson(kpiSummary));

        // 2. 时序分析（所有维度）
        List<String> allDimensions = Arrays.asList("rsrp", "sinr", "macThroughput", "mcs", "prbNum", "rank", "bler");
        Map<String, Object> timeseries = analysisService.getTimeSeriesData(allDimensions);
        result.setTimeseriesJson(toJson(timeseries));

        // 3. 速率分布
        Map<String, Object> distribution = analysisService.getRateDistribution(null);
        result.setDistributionJson(toJson(distribution));

        // 4. 峰值速率计算（默认使用 FDD 100MHz 10下行slots）
        Map<String, Object> peakRate = buildDefaultPeakRate();
        result.setPeakRateJson(toJson(peakRate));

        RuleAnalysisResult saved = ruleAnalysisResultRepository.save(result);
        log.info("[RuleAnalysis] 规则分析完成，resultId={}", saved.getId());
        return saved;
    }

    /**
     * 根据任务ID查询规则分析结果
     */
    public Optional<RuleAnalysisResult> findByTaskId(Long taskId) {
        return ruleAnalysisResultRepository.findByTaskId(taskId);
    }

    // -------------------------------------------------------------------------

    private Map<String, Object> buildKpiSummary() {
        Map<String, Object> summary = new HashMap<>();
        try {
            long total = testDataRepository.countTotalRecords();
            summary.put("total_records", total);

            // MAC 吞吐量统计
            List<Double> throughputs = testDataRepository.findAllMacThroughput();
            if (!throughputs.isEmpty()) {
                double avg = throughputs.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                double max = throughputs.stream().mapToDouble(Double::doubleValue).max().orElse(0);
                double min = throughputs.stream().mapToDouble(Double::doubleValue).min().orElse(0);
                double p50 = percentile(throughputs, 50);
                double p95 = percentile(throughputs, 95);
                summary.put("DL_throughput_avg_Mbps", round(avg, 2));
                summary.put("DL_throughput_max_Mbps", round(max, 2));
                summary.put("DL_throughput_min_Mbps", round(min, 2));
                summary.put("DL_throughput_p50_Mbps", round(p50, 2));
                summary.put("DL_throughput_p95_Mbps", round(p95, 2));
            }
        } catch (Exception e) {
            log.warn("[RuleAnalysis] KPI汇总计算失败: {}", e.getMessage());
            summary.put("error", "KPI 计算异常: " + e.getMessage());
        }
        return summary;
    }

    private Map<String, Object> buildDefaultPeakRate() {
        Map<String, Object> peakRateResult = new HashMap<>();
        try {
            // FDD 100MHz 默认配置
            double fddPeakRate = analysisService.calculatePeakRate("FDD", null, 100.0, 10, null, null, null);
            peakRateResult.put("FDD_100MHz_theoretical_peak_Mbps", round(fddPeakRate, 2));

            // TDD 100MHz 默认配置（8DL + 2特殊）
            double tddPeakRate = analysisService.calculatePeakRate("TDD", "FRAME1", 100.0, 8, 2, null, null);
            peakRateResult.put("TDD_100MHz_theoretical_peak_Mbps", round(tddPeakRate, 2));
        } catch (Exception e) {
            log.warn("[RuleAnalysis] 峰值速率计算失败: {}", e.getMessage());
            peakRateResult.put("error", "峰值速率计算异常: " + e.getMessage());
        }
        return peakRateResult;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("[RuleAnalysis] JSON序列化失败: {}", e.getMessage());
            return "{}";
        }
    }

    private double percentile(List<Double> sorted, int p) {
        List<Double> s = sorted.stream().sorted().toList();
        int idx = (int) Math.ceil(p / 100.0 * s.size()) - 1;
        return s.get(Math.max(0, Math.min(idx, s.size() - 1)));
    }

    private double round(double v, int places) {
        double factor = Math.pow(10, places);
        return Math.round(v * factor) / factor;
    }
}
