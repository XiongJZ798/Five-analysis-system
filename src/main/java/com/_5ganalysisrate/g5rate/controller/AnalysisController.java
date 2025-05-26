package com._5ganalysisrate.g5rate.controller;

import com._5ganalysisrate.g5rate.dto.ApiResponse;
import com._5ganalysisrate.g5rate.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true", maxAge = 3600)
public class AnalysisController {

    private final AnalysisService analysisService;
    private static final Logger log = LoggerFactory.getLogger(AnalysisController.class);

    @PostMapping("/time-series-chart")
    public ApiResponse<?> getTimeSeriesData(@RequestBody Map<String, List<String>> request) {
        try {
            // 基本参数验证
            List<String> dimensions = request.get("dimensions");
            if (dimensions == null || dimensions.isEmpty()) {
                return ApiResponse.error(400, "请选择至少一个分析维度");
            }

            log.info("接收到多维度分析请求，维度: {}", dimensions);

            // 性能监控
            long startTime = System.nanoTime();
            Map<String, Object> result = analysisService.getTimeSeriesData(dimensions);
            long endTime = System.nanoTime();
            long duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

            // 记录性能信息
            log.info("多维度分析API耗时: {}ms, 维度: {}, 数据点数: {}",
                duration,
                dimensions,
                result.containsKey("series") ?
                    ((List<Map<String, Object>>)result.get("series")).stream()
                        .mapToInt(m -> ((List<Object[]>)m.get("data")).size())
                        .sum() : 0
            );

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("多维度分析数据获取失败", e);
            // 提供更详细的错误信息
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = "处理请求时发生未知错误";
            }
            return ApiResponse.error("获取时序数据失败: " + errorMsg);
        }
    }

    @PostMapping("/rate-distribution-chart")
    public ApiResponse<?> getRateDistribution(@RequestBody(required = false) Map<String, List<Map<String, Double>>> request) {
        try {
            // 性能监控
            long startTime = System.nanoTime();

            List<Map<String, Double>> ranges = request != null ? request.get("ranges") : null;
            log.info("收到速率分布分析请求，自定义区间: {}", ranges != null ? ranges.size() : "未提供");

            if (ranges != null && !ranges.isEmpty()) {
                // 记录自定义区间信息
                StringBuilder rangeInfo = new StringBuilder("自定义区间详情: ");
                for (Map<String, Double> range : ranges) {
                    double min = range.get("min");
                    Double max = range.get("max");
                    String rangeStr = max != null ? String.format("%.1f-%.1f", min, max) : String.format("%.1f+", min);
                    rangeInfo.append(rangeStr).append(", ");
                }
                log.info(rangeInfo.toString());
            }

            Map<String, Object> result = analysisService.getRateDistribution(ranges);

            long endTime = System.nanoTime();
            long duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

            // 记录性能信息
            log.info("速率分布API耗时: {}ms, 自定义区间: {}, 返回数据点: {}",
                duration,
                (ranges != null),
                ((List<?>)result.get("pieData")).size());

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("获取速率分布数据失败", e);
            // 提供更详细的错误信息
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = "处理请求时发生未知错误";
            }
            return ApiResponse.error("获取速率分布数据失败: " + errorMsg);
        }
    }

    @PostMapping("/calculate-peak-rate")
    public ApiResponse<?> calculatePeakRate(@RequestBody Map<String, Object> request) {
        try {
            // 参数验证
            if (!request.containsKey("mode") || !request.containsKey("bandwidth") ||
                !request.containsKey("dlSlots")) {
                return ApiResponse.error(400, "缺少必要参数");
            }

            String mode = (String) request.get("mode");
            String frameType = (String) request.get("frameType");
            double bandwidth = ((Number) request.get("bandwidth")).doubleValue();

            // 获取下行时隙数（FDD和TDD模式都需要）
            Integer dlSlots = ((Number) request.get("dlSlots")).intValue();

            // 特殊时隙数仅在TDD模式下需要
            Integer specialSlots = null;
            if ("TDD".equals(mode)) {
                if (!request.containsKey("specialSlots")) {
                    return ApiResponse.error(400, "TDD模式下必须提供特殊时隙数");
                }
                specialSlots = ((Number) request.get("specialSlots")).intValue();
            }

            // 获取可选参数：调制阶数和DL流数
            Integer modulationOrder = request.containsKey("modulationOrder") ?
                ((Number) request.get("modulationOrder")).intValue() : null;

            Integer dlStreams = request.containsKey("dlStreams") ?
                ((Number) request.get("dlStreams")).intValue() : null;

            double result = analysisService.calculatePeakRate(
                mode, frameType, bandwidth, dlSlots, specialSlots, modulationOrder, dlStreams);
            return ApiResponse.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            // 提供更详细的错误信息
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = "处理请求时发生未知错误";
            }
            return ApiResponse.error("计算峰值速率失败: " + errorMsg);
        }
    }

    /**
     * 获取峰值速率计算历史 - 已禁用
     * @param page 页码(从0开始)
     * @param size 每页大小
     * @return 分页的历史记录
     */
    /*
    @GetMapping("/peak-rate-history")
    public ApiResponse<?> getPeakRateHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            // 性能监控
            long startTime = System.nanoTime();

            Page<PeakRateHistory> historyPage = analysisService.getPeakRateHistory(page, size);

            long endTime = System.nanoTime();
            long duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

            // 记录性能信息
            log.info("获取峰值速率历史API耗时: {}ms, 页码: {}, 每页大小: {}", duration, page, size);

            Map<String, Object> result = new HashMap<>();
            result.put("content", historyPage.getContent());
            result.put("totalElements", historyPage.getTotalElements());
            result.put("totalPages", historyPage.getTotalPages());
            result.put("size", historyPage.getSize());
            result.put("number", historyPage.getNumber());

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("获取峰值速率历史失败", e);
            return ApiResponse.error("获取历史记录失败: " + e.getMessage());
        }
    }
    */

    /**
     * 速率对比分析 - 计算理论速率并与MAC层速率对比
     */
    @PostMapping("/rate-comparison")
    public ApiResponse<?> getRateComparison(@RequestBody Map<String, Object> request) {
        try {
            // 参数验证
            String mode = (String) request.get("mode");
            if (mode == null) {
                return ApiResponse.error(400, "缺少计算模式参数");
            }

            Integer dlSlots = ((Number) request.get("dlSlots")).intValue();
            if (dlSlots == null || dlSlots <= 0) {
                return ApiResponse.error(400, "下行slot数必须大于0");
            }

            // TDD模式下的特殊参数验证
            String frameType = null;
            Integer specialSlots = null;
            if ("TDD".equals(mode)) {
                frameType = (String) request.get("frameType");
                if (frameType == null) {
                    return ApiResponse.error(400, "TDD模式下必须提供帧结构类型");
                }

                specialSlots = request.containsKey("specialSlots") ?
                    ((Number) request.get("specialSlots")).intValue() : null;
                if (specialSlots == null) {
                    return ApiResponse.error(400, "TDD模式下必须提供特殊slot数");
                }

                if (specialSlots < 0 || specialSlots > 14) {
                    return ApiResponse.error(400, "特殊slot数必须在0-14之间");
                }

                if (dlSlots + specialSlots > 14) {
                    return ApiResponse.error(400, "下行slot数和特殊slot数之和不能超过14");
                }
            }

            // 调用服务获取对比数据
            Map<String, Object> result = analysisService.calculateRateComparison(
                mode, frameType, dlSlots, specialSlots);

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("速率对比分析失败", e);
            return ApiResponse.error("速率对比分析失败: " + e.getMessage());
        }
    }
}