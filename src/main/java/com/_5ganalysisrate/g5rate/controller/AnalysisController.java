package com._5ganalysisrate.g5rate.controller;

import com._5ganalysisrate.g5rate.dto.ApiResponse;
import com._5ganalysisrate.g5rate.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping("/time-series-chart")
    public ApiResponse<?> getTimeSeriesData(@RequestBody Map<String, List<String>> request) {
        try {
            List<String> dimensions = request.get("dimensions");
            if (dimensions == null || dimensions.isEmpty()) {
                return ApiResponse.error(400, "请选择至少一个分析维度");
            }
            return ApiResponse.success(analysisService.getTimeSeriesData(dimensions));
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("获取时序数据失败: " + e.getMessage());
        }
    }

    @PostMapping("/rate-distribution-chart")
    public ApiResponse<?> getRateDistribution(@RequestBody(required = false) List<Map<String, Double>> ranges) {
        try {
            return ApiResponse.success(analysisService.getRateDistribution(ranges));
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("获取速率分布数据失败: " + e.getMessage());
        }
    }

    @PostMapping("/calculate-peak-rate")
    public ApiResponse<?> calculatePeakRate(@RequestBody Map<String, Object> request) {
        try {
            String mode = (String) request.get("mode");
            String frameType = (String) request.get("frameType");
            double bandwidth = ((Number) request.get("bandwidth")).doubleValue();

            // 获取下行时隙数（FDD和TDD模式都需要）
            Integer dlSlots = ((Number) request.get("dlSlots")).intValue();
            
            // 特殊时隙数仅在TDD模式下需要
            Integer specialSlots = null;
            if ("TDD".equals(mode)) {
                specialSlots = ((Number) request.get("specialSlots")).intValue();
            }

            return ApiResponse.success(
                analysisService.calculatePeakRate(mode, frameType, bandwidth, dlSlots, specialSlots)
            );
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("计算峰值速率失败: " + e.getMessage());
        }
    }
} 