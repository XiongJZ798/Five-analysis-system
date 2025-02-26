package com._5ganalysisrate.g5rate.service;

import com._5ganalysisrate.g5rate.model.TestData;
import com._5ganalysisrate.g5rate.repository.TestDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final TestDataRepository testDataRepository;
    
    public Map<String, Object> getTimeSeriesData(List<String> dimensions) {
        List<TestData> allData = testDataRepository.findAllByOrderByTestTimeAsc();
        
        if (allData.isEmpty()) {
            throw new IllegalStateException("没有可用的数据");
        }

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> series = new ArrayList<>();
        List<String> legend = new ArrayList<>();

        // 计算数据点的时间间隔，确保数据点足够密集但不会过多
        long totalPoints = Math.min(allData.size(), 1000); // 最多显示1000个数据点
        int step = Math.max(1, allData.size() / (int)totalPoints);

        for (String dimension : dimensions) {
            Map<String, Object> seriesData = new HashMap<>();
            seriesData.put("name", dimension);
            
            // 使用步长采样数据点
            List<Object[]> data = IntStream.range(0, allData.size())
                .filter(i -> i % step == 0)
                .mapToObj(i -> new Object[]{
                    allData.get(i).getTestTime().toString(),
                    getDimensionValue(allData.get(i), dimension)
                })
                .collect(Collectors.toList());
            
            seriesData.put("data", data);
            series.add(seriesData);
            legend.add(dimension);
        }

        result.put("series", series);
        result.put("legend", legend);
        return result;
    }

    public Map<String, Object> getRateDistribution(List<Map<String, Double>> customRanges) {
        List<TestData> allData = testDataRepository.findAll();
        List<Double> allRates = allData.stream()
            .map(TestData::getMacThroughput)
            .collect(Collectors.toList());

        List<Map<String, Object>> pieData;
        if (customRanges != null && !customRanges.isEmpty()) {
            pieData = calculateCustomRangeDistribution(allRates, customRanges);
        } else {
            pieData = calculateDefaultRangeDistribution(allRates);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("pieData", pieData);
        return result;
    }

    private List<Map<String, Object>> calculateDefaultRangeDistribution(List<Double> rates) {
        // 根据数据范围动态生成更细分的区间
        double maxRate = rates.stream()
            .mapToDouble(Double::doubleValue)
            .max()
            .orElse(500.0);
        
        // 根据最大速率生成合适的区间范围
        List<Double> boundaries = new ArrayList<>();
        if (maxRate <= 100) {
            // 小于100Mbps时，每20Mbps一个区间
            for (double i = 0; i <= 100; i += 20) {
                boundaries.add(i);
            }
        } else if (maxRate <= 500) {
            // 100-500Mbps范围，每50Mbps一个区间
            for (double i = 0; i <= 500; i += 50) {
                boundaries.add(i);
            }
        } else {
            // 大于500Mbps时，每100Mbps一个区间
            for (double i = 0; i <= maxRate + 100; i += 100) {
                boundaries.add(i);
            }
        }
        
        return IntStream.range(0, boundaries.size() - 1)
            .mapToObj(i -> {
                double min = boundaries.get(i);
                double max = boundaries.get(i + 1);
                String label = String.format("%.0f-%.0f Mbps", min, max);

                long count = rates.stream()
                    .filter(rate -> rate >= min && rate < max)
                    .count();

                if (count > 0) {
                    Map<String, Object> slice = new HashMap<>();
                    slice.put("name", label);
                    slice.put("value", count * 100.0 / rates.size());
                    return slice;
                }
                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private List<Map<String, Object>> calculateCustomRangeDistribution(
            List<Double> rates, List<Map<String, Double>> ranges) {
        List<Map<String, Object>> pieData = new ArrayList<>();
        
        for (Map<String, Double> range : ranges) {
            double min = range.get("min");
            Double max = range.get("max");
            String label = max != null ? 
                String.format("%.0f-%.0f Mbps", min, max) :
                String.format("%.0f+ Mbps", min);

            long count = rates.stream()
                .filter(rate -> rate >= min && (max == null || rate < max))
                .count();
            
            if (count > 0) {
                Map<String, Object> slice = new HashMap<>();
                slice.put("name", label);
                slice.put("value", count * 100.0 / rates.size());
                pieData.add(slice);
            }
        }
        
        return pieData;
    }

    /**
     * 计算5G下行峰值速率公式
     * @param mode 计算模式（FDD/TDD）
     * @param frameType TDD模式下的帧结构类型
     * @param bandwidth 系统带宽(MHz)
     * @param dlSlots 下行时隙数
     * @param specialSlots 特殊时隙数
     * @return 峰值速率(Gbit/s)
     */
    public double calculatePeakRate(String mode, String frameType, double bandwidth,
                                  Integer dlSlots, Integer specialSlots) {
        // 固定参数
        final int MODULATION_ORDER = 8;    // 调制阶数固定为8
        final int DL_STREAMS = 4;          // DL流数固定为4
        final int SCS = 30;                // 子载波间隔
        final int GUARD_INTERVAL = 845;    // 保护间隔
        final double CODING_EFFICIENCY = 0.925;  // 编码效率
        
        // 计算RE (Resource Element)
        final int RE = 12 * (14 - 1);  // RE = 12*(14-1)
        
        // 计算PRB (Physical Resource Block)
        int PRB = (int) Math.floor((bandwidth * 1000 - 2 * GUARD_INTERVAL) / (12 * SCS));
        
        // 计算Nifo
        double Nifo = Math.floor(RE * PRB * MODULATION_ORDER * DL_STREAMS * CODING_EFFICIENCY);
        
        // 计算Nifo01
        double Nifo01 = Math.pow(2, 15) * Math.floor((Nifo - 24) / Math.pow(2, 15));
        
        // 计算TBS (Transport Block Size)
        double TBS = 8 * MODULATION_ORDER * Math.floor((Nifo01 + 24) / (8 * MODULATION_ORDER));
        
        // 根据模式计算最终速率
        if ("FDD".equals(mode)) {
            // 验证下行时隙参数
            if (dlSlots == null) {
                throw new IllegalArgumentException("FDD模式下必须提供下行时隙数");
            }
            if (dlSlots <= 0) {
                throw new IllegalArgumentException("下行时隙数必须大于0");
            }
            
            // FDD模式：使用固定10ms周期
            return calculateFinalRate(TBS, dlSlots, 0, 10.0);
        } else if ("TDD".equals(mode)) {
            if (frameType == null) {
                throw new IllegalArgumentException("TDD模式下必须指定帧结构类型");
            }

            // 验证时隙参数
            if (dlSlots == null || specialSlots == null) {
                throw new IllegalArgumentException("TDD模式下必须提供下行时隙数和特殊时隙数");
            }
            // 验证参数
            if (dlSlots <= 0) {
                throw new IllegalArgumentException("下行时隙数必须大于0");
            }
            if (specialSlots < 0 || specialSlots > 14) {
                throw new IllegalArgumentException("特殊时隙数必须在0-14之间");
            }
            if (dlSlots + specialSlots > 14) {
                throw new IllegalArgumentException("下行时隙数和特殊时隙数之和不能超过14");
            }

            // 获取帧结构对应的周期
            double period = getTDDFramePeriod(frameType);
            return calculateFinalRate(TBS, dlSlots, specialSlots, period);
        } else {
            throw new IllegalArgumentException("不支持的计算模式: " + mode);
        }
    }

    /**
     * 计算最终速率
     * @param TBS 传输块大小
     * @param dlSlots 下行时隙数
     * @param specialSlots 特殊时隙数
     * @param period 周期（ms）
     * @return 峰值速率(Gbit/s)
     */
    private double calculateFinalRate(double TBS, int dlSlots, int specialSlots, double period) {
        return (TBS * ((dlSlots + specialSlots) * 1000) / period) / Math.pow(10, 9);
    }

    /**
     * 获取TDD帧结构的周期
     */
    private double getTDDFramePeriod(String frameType) {
        return switch (frameType) {
            case "FRAME1" -> 2.0;    // 2ms帧结构
            case "FRAME2" -> 2.5;    // 2.5ms帧结构
            case "FRAME3" -> 2.5;    // 2.5ms双帧结构
            case "FRAME4" -> 5.0;    // 5ms帧结构
            case "FRAME5" -> 1.0;    // 1ms帧结构
            default -> throw new IllegalArgumentException("不支持的帧结构类型: " + frameType);
        };
    }

    private Object getDimensionValue(TestData data, String dimension) {
        return switch (dimension) {
            case "rsrp" -> data.getRsrp();
            case "sinr" -> data.getSinr();
            case "macThroughput" -> data.getMacThroughput();
            case "rank" -> data.getRank();
            case "mcs" -> data.getMcs();
            case "rbNum" -> data.getRbNum();
            case "bler" -> data.getBler();
            default -> throw new IllegalArgumentException("未知的维度: " + dimension);
        };
    }
} 