package com._5ganalysisrate.g5rate.service;

import com._5ganalysisrate.g5rate.model.TestData;
import com._5ganalysisrate.g5rate.repository.TestDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final TestDataRepository testDataRepository;

    // 添加缓存以减少数据库查询
    // 移除@Cacheable注解，确保每次都从数据库获取最新数据
    public List<TestData> getTimeSeriesDataCache() {
        try {
            // 限制数据量，防止OOM错误
            return testDataRepository.findAllByOrderByTestTimeAsc().stream()
                    .limit(5000)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // 记录异常，返回空列表
            log.error("获取时序数据缓存失败", e);
            return Collections.emptyList();
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTimeSeriesData(List<String> dimensions) {
        try {
            log.info("开始获取多维度分析数据，维度: {}", dimensions);

            // 使用分页方式获取数据，减少内存压力，并按创建时间降序排序以获取最新数据
            int pageSize = 2000; // 增加页面大小以获取更多数据

            // 清除缓存，确保获取最新数据
            log.info("清除缓存，确保获取最新数据");

            // 按创建时间降序排序，确保获取最新上传的数据
            List<TestData> allData = testDataRepository.findAll(PageRequest.of(0, pageSize, Sort.by("createTime").descending()))
                    .getContent();

            log.info("从数据库获取到 {} 条数据记录", allData.size());

            if (allData.isEmpty()) {
                log.warn("没有找到测试数据");
                return Map.of(
                    "series", Collections.emptyList(),
                    "legend", dimensions
                );
            }

            log.info("找到{}条测试数据，准备进行多维度分析", allData.size());

            Map<String, Object> result = new HashMap<>();
            List<Map<String, Object>> series = new ArrayList<>();

            // 优化数据采样 - 最多返回2000个点，确保关键点被保留
            int maxPoints = 2000;
            int samplingInterval = Math.max(1, allData.size() / maxPoints);

            log.info("数据点总数: {}, 采样间隔: {}, 预计采样后点数: ~{}",
                allData.size(), samplingInterval, allData.size() / samplingInterval);

            for (String dimension : dimensions) {
                Map<String, Object> seriesData = new HashMap<>();
                seriesData.put("name", dimension);

                // 采样数据并保留关键点（极值点）
                List<Object[]> data = new ArrayList<>();
                
                // 获取单一维度的统计信息用于日志
                if ("rsrp".equals(dimension)) {
                    DoubleSummaryStatistics rsrpStats = allData.stream()
                        .map(TestData::getRsrp)
                        .filter(Objects::nonNull)
                        .mapToDouble(Double::doubleValue)
                        .summaryStatistics();
                    log.info("RSRP数据统计: 最小值={}, 最大值={}, 平均值={}, 数据点数={}",
                        rsrpStats.getMin(), rsrpStats.getMax(), rsrpStats.getAverage(), rsrpStats.getCount());
                }

                // 为每个维度计算关键数据点
                if (samplingInterval > 1) {
                    List<TestData> sampledData = new ArrayList<>();

                    // 常规采样
                    for (int i = 0; i < allData.size(); i += samplingInterval) {
                        sampledData.add(allData.get(i));
                    }

                    // 确保最后一个点被包含
                    if (allData.size() > 0 && !sampledData.contains(allData.get(allData.size() - 1))) {
                        sampledData.add(allData.get(allData.size() - 1));
                    }

                    // 查找关键点：局部最大/最小值和快速变化的区域
                    if (samplingInterval > 2) { // 只在较大间隔时添加关键点
                        List<TestData> keyPoints = findKeyPoints(allData, dimension);
                        log.info("为维度 {} 找到 {} 个关键点", dimension, keyPoints.size());

                        // 合并关键点和采样点，按时间排序
                        sampledData.addAll(keyPoints);
                        sampledData = sampledData.stream()
                            .distinct()
                            .sorted(Comparator.comparing(TestData::getTestTime))
                            .collect(Collectors.toList());
                    }

                    // 处理采样后的数据
                    for (TestData testData : sampledData) {
                        Object value = getDimensionValue(testData, dimension);
                        if (value != null) {
                            data.add(new Object[]{
                                testData.getTestTime().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")),
                                value
                            });
                        }
                    }
                } else {
                    // 如果数据量不大，使用所有数据点
                    for (TestData testData : allData) {
                        Object value = getDimensionValue(testData, dimension);
                        if (value != null) {
                            data.add(new Object[]{
                                testData.getTestTime().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")),
                                value
                            });
                        }
                    }
                }

                log.debug("维度 {} 最终数据点数: {}", dimension, data.size());
                
                // 如果是RSRP维度，确保采样后的数据包含整个范围的值
                if ("rsrp".equals(dimension)) {
                    log.info("RSRP维度最终采样: 数据点数={}", data.size());
                    // 输出部分采样数据点用于调试
                    if (!data.isEmpty()) {
                        int logCount = Math.min(5, data.size());
                        for (int i = 0; i < logCount; i++) {
                            log.debug("RSRP采样点{}: 时间={}, 值={}dBm", 
                                i, data.get(i)[0], data.get(i)[1]);
                        }
                        // 输出最后几个点
                        for (int i = Math.max(0, data.size() - logCount); i < data.size(); i++) {
                            log.debug("RSRP采样点{}: 时间={}, 值={}dBm", 
                                i, data.get(i)[0], data.get(i)[1]);
                        }
                    }
                }
                
                seriesData.put("data", data);
                series.add(seriesData);
            }

            result.put("series", series);
            result.put("legend", dimensions);
            return result;
        } catch (Exception e) {
            log.error("获取时序数据失败", e);
            throw new RuntimeException("获取时序数据失败: " + e.getMessage());
        }
    }

    /**
     * 查找数据中的关键点（局部最大/最小值和快速变化的点）
     */
    private List<TestData> findKeyPoints(List<TestData> allData, String dimension) {
        List<TestData> keyPoints = new ArrayList<>();
        int windowSize = 5; // 滑动窗口大小
        
        // 对RSRP维度使用更小的窗口大小和更敏感的检测
        if ("rsrp".equals(dimension)) {
            windowSize = 3; // RSRP维度使用更小的窗口
        }

        if (allData.size() <= windowSize * 2) {
            return keyPoints; // 数据点太少，不需要查找关键点
        }
        
        // 获取所有维度数据的统计信息，用于计算变化阈值
        List<Double> dimensionValues = new ArrayList<>();
        for (TestData data : allData) {
            Object value = getDimensionValue(data, dimension);
            if (value != null && value instanceof Number) {
                dimensionValues.add(((Number) value).doubleValue());
            }
        }
        
        // 如果没有有效数据，返回空列表
        if (dimensionValues.isEmpty()) {
            return keyPoints;
        }
        
        // 计算维度数据的统计信息
        DoubleSummaryStatistics stats = dimensionValues.stream()
            .mapToDouble(Double::doubleValue)
            .summaryStatistics();
        double min = stats.getMin();
        double max = stats.getMax();
        double range = max - min;
        
        // 对RSRP维度进行特殊处理：识别特定值范围的数据点
        if ("rsrp".equals(dimension)) {
            log.info("RSRP维度数据范围: {}dBm 到 {}dBm", min, max);
            
            // 如果有-80dBm附近的值，确保将其作为关键点添加
            for (int i = 0; i < allData.size(); i++) {
                TestData data = allData.get(i);
                Double rsrpValue = data.getRsrp();
                
                if (rsrpValue != null) {
                    // 特别关注-80dBm附近的值（增加标记该区域的关键点）
                    if (rsrpValue >= -82 && rsrpValue <= -78) {
                        keyPoints.add(data);
                        log.debug("添加特定范围RSRP关键点: {}dBm, 时间: {}", rsrpValue, data.getTestTime());
                    }
                    
                    // 每隔1dBm的数据也作为关键点（确保均匀分布的关键点）
                    if (i % 3 == 0 && rsrpValue > -90 && rsrpValue < -70) {
                        keyPoints.add(data);
                    }
                }
            }
        }

        // 查找局部最大/最小值
        for (int i = windowSize; i < allData.size() - windowSize; i++) {
            TestData current = allData.get(i);
            Object currentValue = getDimensionValue(current, dimension);
            if (currentValue == null || !(currentValue instanceof Number)) {
                continue;
            }

            double value = ((Number) currentValue).doubleValue();
            boolean isLocalMax = true;
            boolean isLocalMin = true;

            // 检查是否为局部最大/最小值
            for (int j = i - windowSize; j <= i + windowSize; j++) {
                if (j == i) continue;

                Object neighborValue = getDimensionValue(allData.get(j), dimension);
                if (neighborValue == null || !(neighborValue instanceof Number)) {
                    continue;
                }

                double nValue = ((Number) neighborValue).doubleValue();
                if (nValue > value) isLocalMax = false;
                if (nValue < value) isLocalMin = false;

                if (!isLocalMax && !isLocalMin) break;
            }

            if (isLocalMax || isLocalMin) {
                keyPoints.add(current);
                if ("rsrp".equals(dimension)) {
                    log.debug("添加RSRP局部{}值: {}dBm, 时间: {}", 
                        isLocalMax ? "最大" : "最小", value, current.getTestTime());
                }
            }
        }

        // 查找快速变化的点
        for (int i = 1; i < allData.size() - 1; i++) {
            Object prevValue = getDimensionValue(allData.get(i-1), dimension);
            Object currentValue = getDimensionValue(allData.get(i), dimension);
            Object nextValue = getDimensionValue(allData.get(i+1), dimension);

            if (prevValue == null || currentValue == null || nextValue == null ||
                !(prevValue instanceof Number) ||
                !(currentValue instanceof Number) ||
                !(nextValue instanceof Number)) {
                continue;
            }

            double prev = ((Number) prevValue).doubleValue();
            double current = ((Number) currentValue).doubleValue();
            double next = ((Number) nextValue).doubleValue();

            // 计算变化率（根据维度类型调整敏感度）
            double thresholdRatio = 0.2; // 默认变化率阈值
            if ("rsrp".equals(dimension)) {
                // RSRP范围通常在-140到-44dBm，需要更敏感的阈值
                // 对于RSRP使用绝对变化值而不是相对变化率
                double absDiff = Math.abs(current - prev);
                if (absDiff >= 1.0) { // 如果RSRP变化大于等于1dBm，标记为关键点
                    keyPoints.add(allData.get(i));
                    log.debug("添加RSRP快速变化点: 从{}dBm到{}dBm (变化{}dBm), 时间: {}", 
                        prev, current, absDiff, allData.get(i).getTestTime());
                    continue;
                }
            } else {
                // 计算变化率
                double ratePrev = Math.abs((current - prev) / Math.max(0.1, Math.abs(prev)));
                double rateNext = Math.abs((next - current) / Math.max(0.1, Math.abs(current)));
    
                // 如果任一变化率超过阈值，认为是关键点
                if (ratePrev > thresholdRatio || rateNext > thresholdRatio) {
                    keyPoints.add(allData.get(i));
                }
            }
        }

        // 对于RSRP维度，如果关键点太少，增加额外的采样点
        if ("rsrp".equals(dimension) && keyPoints.size() < allData.size() / 10) {
            int additionalSamplingInterval = Math.max(1, allData.size() / 200);
            log.info("RSRP关键点数量不足，增加额外采样: 每{}个点取样", additionalSamplingInterval);
            
            for (int i = 0; i < allData.size(); i += additionalSamplingInterval) {
                TestData data = allData.get(i);
                if (data.getRsrp() != null) {
                    keyPoints.add(data);
                }
            }
        }

        return keyPoints.stream().distinct().collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRateDistribution(List<Map<String, Double>> customRanges) {
        try {
            log.info("开始获取速率分布数据");

            int pageSize = 1000;
            int currentPage = 0;
            List<Double> rates = new ArrayList<>();

            // 清除缓存，确保获取最新数据
            log.info("清除缓存，确保获取最新速率数据");

            while (true) {
                // 按创建时间降序排序，确保获取最新上传的数据
                Page<TestData> page = testDataRepository.findAll(
                    PageRequest.of(currentPage, pageSize, Sort.by("createTime").descending())
                );

                List<Double> pageRates = page.getContent().stream()
                    .map(TestData::getMacThroughput)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

                rates.addAll(pageRates);
                log.info("已获取 {} 条速率数据", rates.size());

                if (!page.hasNext()) {
                    break;
                }
                currentPage++;

                // 设置一个合理的上限以防数据量过大
                if (rates.size() >= 10000) {
                    log.warn("数据量超过10000条，将只处理前10000条数据");
                    break;
                }
            }

            if (rates.isEmpty()) {
                Map<String, Object> emptyResult = new HashMap<>();
                emptyResult.put("pieData", new ArrayList<>());
                return emptyResult;
            }

            List<Map<String, Object>> pieData;
            if (customRanges != null && !customRanges.isEmpty()) {
                pieData = calculateCustomRangeDistribution(rates, customRanges);
            } else {
                pieData = calculateDefaultRangeDistribution(rates);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("pieData", pieData);
            return result;
        } catch (Exception e) {
            log.error("获取速率分布数据失败", e);
            throw new RuntimeException("获取速率分布数据失败: " + e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> calculateDefaultRangeDistribution(List<Double> rates) {
        try {
            // 使用与前端一致的固定速率区间
            List<Map<String, Object>> pieData = new ArrayList<>();

            // 输出数据分布情况用于调试
            log.info("速率数据总数: {}", rates.size());
            DoubleSummaryStatistics stats = rates.stream()
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();
            log.info("速率数据统计: 最小值={}, 最大值={}, 平均值={}",
                stats.getMin(), stats.getMax(), stats.getAverage());

            // 定义固定的速率区间（与前端完全匹配）
            List<double[]> fixedRanges = new ArrayList<>();

            // 当最大速率≤50Mbps时：每10Mbps一个区间
            fixedRanges.add(new double[]{0, 10});      // 0.0-10.0 Mbps
            fixedRanges.add(new double[]{10, 20});     // 10.0-20.0 Mbps
            fixedRanges.add(new double[]{20, 30});     // 20.0-30.0 Mbps
            fixedRanges.add(new double[]{30, 40});     // 30.0-40.0 Mbps
            fixedRanges.add(new double[]{40, 50});     // 40.0-50.0 Mbps

            // 当最大速率在50-100Mbps之间：分两个区间
            fixedRanges.add(new double[]{50, 75});     // 50.0-75.0 Mbps
            fixedRanges.add(new double[]{75, 100});    // 75.0-100.0 Mbps

            // 当最大速率在100-500Mbps之间：每50Mbps一个区间
            fixedRanges.add(new double[]{100, 150});   // 100.0-150.0 Mbps
            fixedRanges.add(new double[]{150, 200});   // 150.0-200.0 Mbps
            fixedRanges.add(new double[]{200, 250});   // 200.0-250.0 Mbps
            fixedRanges.add(new double[]{250, 300});   // 250.0-300.0 Mbps
            fixedRanges.add(new double[]{300, 350});   // 300.0-350.0 Mbps
            fixedRanges.add(new double[]{350, 400});   // 350.0-400.0 Mbps
            fixedRanges.add(new double[]{400, 450});   // 400.0-450.0 Mbps
            fixedRanges.add(new double[]{450, 500});   // 450.0-500.0 Mbps

            // 当最大速率>500Mbps时：每100Mbps一个区间
            fixedRanges.add(new double[]{500, 600});   // 500.0-600.0 Mbps
            fixedRanges.add(new double[]{600, 700});   // 600.0-700.0 Mbps
            fixedRanges.add(new double[]{700, 800});   // 700.0-800.0 Mbps
            fixedRanges.add(new double[]{800, 900});   // 800.0-900.0 Mbps
            fixedRanges.add(new double[]{900, 1000});  // 900.0-1000.0 Mbps
            fixedRanges.add(new double[]{1000, Double.MAX_VALUE}); // 1000.0+ Mbps

            // 计算每个区间的数据占比
            for (double[] range : fixedRanges) {
                String label;
                if (range[1] == Double.MAX_VALUE) {
                    label = String.format("%.1f+ Mbps", range[0]);
                } else {
                    label = String.format("%.1f-%.1f Mbps", range[0], range[1]);
                }

                long count = rates.stream()
                    .filter(rate -> rate >= range[0] && rate < range[1])
                    .count();

                if (count > 0) {
                    Map<String, Object> slice = new HashMap<>();
                    slice.put("name", label);
                    slice.put("value", count * 100.0 / rates.size());
                    pieData.add(slice);
                }
            }

            return pieData;
        } catch (Exception e) {
            log.error("计算默认速率区间分布失败", e);
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> calculateCustomRangeDistribution(
            List<Double> rates, List<Map<String, Double>> ranges) {
        try {
            List<Map<String, Object>> pieData = new ArrayList<>();

            // 输出数据分布情况用于调试
            log.info("速率数据总数: {}", rates.size());
            DoubleSummaryStatistics stats = rates.stream()
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();
            log.info("接收到自定义速率区间划分请求。速率数据统计: 最小值={}, 最大值={}, 平均值={}",
                stats.getMin(), stats.getMax(), stats.getAverage());
            log.info("自定义区间数量: {}", ranges.size());

            for (Map<String, Double> range : ranges) {
                try {
                    double min = range.get("min");
                    Double max = range.get("max");

                    // 格式化标签，处理无上限的情况
                    String label;
                    if (max == null) {
                        label = String.format("%.1f+ Mbps", min);
                        log.info("处理无上限区间: {}", label);
                    } else {
                        label = String.format("%.1f-%.1f Mbps", min, max);
                    }

                    // 计算该区间内的数据数量
                    long count;
                    if (max == null) {
                        // 无上限区间
                        count = rates.stream()
                            .filter(rate -> rate >= min)
                            .count();
                    } else {
                        // 普通区间
                        count = rates.stream()
                            .filter(rate -> rate >= min && rate < max)
                            .count();
                    }

                    // 计算百分比
                    double percentage = count * 100.0 / rates.size();
                    log.info("区间 [{}]: 数量={}, 百分比={}%", label, count, percentage);

                    // 只有当区间内有数据时才添加到结果中
                    if (count > 0) {
                        Map<String, Object> slice = new HashMap<>();
                        slice.put("name", label);
                        slice.put("value", percentage);
                        pieData.add(slice);
                    }
                } catch (Exception e) {
                    log.error("处理区间时出错", e);
                }
            }

            return pieData;
        } catch (Exception e) {
            log.error("计算自定义速率区间分布失败", e);
            // 出错时返回空列表而非抛出异常
            return new ArrayList<>();
        }
    }

    /**
     * 计算5G下行峰值速率公式
     * @param mode 计算模式（FDD/TDD）
     * @param frameType TDD模式下的帧结构类型
     * @param bandwidth 系统带宽(MHz)
     * @param dlSlots 下行时隙数
     * @param specialSlots 特殊时隙数
     * @param modulationOrder 调制阶数
     * @param dlStreams DL流数
     * @return 峰值速率(Gbit/s)
     */
    @Transactional
    public double calculatePeakRate(String mode, String frameType, double bandwidth,
                                    Integer dlSlots, Integer specialSlots, Integer modulationOrder, Integer dlStreams) {
        try {
            // --- Enhanced Parameter Validation ---
            if (bandwidth <= 0) {
                throw new IllegalArgumentException(String.format("系统带宽 %.2f MHz 无效，必须大于 0 MHz", bandwidth));
            }
            if (bandwidth > 1000) {
                throw new IllegalArgumentException(String.format("系统带宽 %.2f MHz 超出最大允许值 (1000 MHz)", bandwidth));
            }
            
            if (dlSlots == null || dlSlots <= 0) {
                throw new IllegalArgumentException("下行时隙数必须大于0且不能为空");
            }
            // 根据计算模式设定dlSlots的合理上限 (示例值，可调整)
            int maxDlSlots = "TDD".equals(mode) ? 14 : 40; // TDD通常基于14个符号，FDD基于更多周期内slot
            if (dlSlots > maxDlSlots) {
                 throw new IllegalArgumentException(String.format("%s模式下，下行时隙数 %d 超出合理上限 (%d)", mode, dlSlots, maxDlSlots));
            }
            
            if ("TDD".equals(mode)) {
                if (frameType == null || frameType.isEmpty()) {
                    throw new IllegalArgumentException("TDD模式下必须指定帧结构类型");
                }
                if (specialSlots == null) {
                    throw new IllegalArgumentException("TDD模式下必须指定特殊时隙数");
                }
                if (specialSlots < 0) {
                    throw new IllegalArgumentException("TDD模式下，特殊时隙数不能为负");
                }
                if (dlSlots + specialSlots > 14) {
                    throw new IllegalArgumentException(String.format("TDD模式下，下行时隙数 (%d) 与特殊时隙数 (%d) 之和不能超过14", dlSlots, specialSlots));
                }
            }
            
            // 默认值设置 - modulationOrder 和 dlStreams 如果为null，则使用默认值
            int currentModulationOrder = (modulationOrder != null) ? modulationOrder : 8; // 默认256QAM (8)
            int currentDlStreams = (dlStreams != null) ? dlStreams : 4;       // 默认4条流
            
            // 调制阶数验证 (基于传递或默认的值)
            // 有效的调制阶数值通常是 2, 4, 6, 8
            List<Integer> validModOrders = Arrays.asList(2, 4, 6, 8);
            if (!validModOrders.contains(currentModulationOrder)) {
                throw new IllegalArgumentException(String.format("调制阶数 %d 无效，有效值应为 2, 4, 6, 或 8", currentModulationOrder));
            }
            
            // DL流数验证 (基于传递或默认的值)
            // 常见的DL流数是 1, 2, 4, 8
            if (currentDlStreams <= 0 || currentDlStreams > 8) { // 假设最大为8层
                throw new IllegalArgumentException(String.format("DL流数 %d 超出有效范围 (1-8)", currentDlStreams));
            }
            // 对较大的DL流数发出警告但继续计算 - 这部分逻辑可以保留或移除，如果严格限制在1-8
            // if (currentDlStreams > 4 && currentDlStreams <=8) { // 例如，超过4层时给个警告
            //     log.warn("DL流数({})较高，请确认设备支持", currentDlStreams);
            // }

            // 固定参数
            final int SCS = 30; // 子载波间隔，单位：kHz
            final int GUARD_INTERVAL = 845; // 恢复原始的保护间隔值
            final double CODING_EFFICIENCY = 0.925; // 编码效率

            // 计算PRB (Physical Resource Block)
            int PRB = (int) Math.floor((bandwidth * 1000 - 2 * GUARD_INTERVAL) / (12 * SCS));
            
            // PRB验证
            if (PRB <= 0) {
                throw new IllegalArgumentException("计算得到的PRB数为0或负数，请检查带宽参数");
            }

            // 计算RE (Resource Element)
            int RE = 12 * (14 - 1);

            // 计算Nifo
            double Nifo = Math.floor(RE * PRB * currentModulationOrder * currentDlStreams * CODING_EFFICIENCY);
            log.debug("峰值速率计算 - RE={}, PRB={}, 调制阶数={}, 层数={}, 编码效率={}, Nifo={}",
                     RE, PRB, currentModulationOrder, currentDlStreams, CODING_EFFICIENCY, Nifo);
            
            // Nifo01 计算逻辑 (更紧凑形式)
            // 1. 计算 n = int(log2(Nifo - 24)) - 5
            double n_val = Math.floor(Math.log(Nifo - 24.0) / Math.log(2.0)) - 5.0;
            
            // 2. 计算 2^n 
            double power_of_2_n = Math.pow(2, n_val);
            
            // 3. 计算用于 MAX 比较的项: (2^n) * int((Nifo - 24) / (2^n))
            double term_for_max;
            if (power_of_2_n == 0.0) { // 保持对 2^n == 0 的检查
                term_for_max = 0.0;
            } else {
                term_for_max = power_of_2_n * Math.floor((Nifo - 24.0) / power_of_2_n);
            }
            
            // 4. Nifo01 = MAX(3840, term_for_max)
            double Nifo01 = Math.max(3840.0, term_for_max);
            
            // 计算TBS = 8 * 调制阶数 * floor((Nifo01 + 24)/(8 * 调制阶数))
            double TBS = 8 * currentModulationOrder * Math.floor((Nifo01 + 24) / (8 * currentModulationOrder));
            log.debug("峰值速率计算 - TBS计算: Nifo01={}, modulationOrder={}, TBS={}", 
                    Nifo01, currentModulationOrder, TBS);

            double result;

            // 根据模式计算最终速率
            if ("FDD".equals(mode)) {
                // FDD模式，固定周期为10ms
                double period = 10.0;
                result = (TBS * (dlSlots * 1000) / period) / Math.pow(10, 9);
            } else {
                // TDD模式，周期根据帧结构类型确定
                double period = getTDDFramePeriod(frameType);

                // 对于TDD，需要考虑特殊时隙
                int specialSlotsValue = specialSlots != null ? specialSlots : 0;

                // 特殊处理FRAME5，当特殊时隙为1时
                if ("FRAME5".equals(frameType) && specialSlotsValue == 1) {
                    // 帧结构5且特殊时隙为1时，类似FDD模式处理
                    result = (TBS * (dlSlots * 1000) / period) / Math.pow(10, 9);
                    log.info("帧结构5(1ms)特殊时隙为1，仅计算下行时隙部分");
                } else {
                    // 计算公式适当调整，考虑特殊时隙
                    result = (TBS * ((dlSlots + specialSlotsValue) * 1000) / period) / Math.pow(10, 9);
                }
            }


            // 记录计算过程和结果日志
            log.info("峰值速率计算: mode={}, frameType={}, bandwidth={}, dlSlots={}, specialSlots={}, " +
                     "modulationOrder={}, dlStreams={}, PRB={}, TBS={}, 结果={} Gbps",
                     mode, frameType, bandwidth, dlSlots, specialSlots, currentModulationOrder, currentDlStreams, PRB, TBS, result);

            return result;
        } catch (Exception e) {
            log.error("峰值速率计算失败: {}", e.getMessage(), e);
            throw new RuntimeException("峰值速率计算失败: " + e.getMessage(), e);
        }
    }




    /**
     * 获取TDD帧结构的周期（ms）
     * 对应关系如下：
     *   帧结构1（2ms）：2
     *   帧结构2（2.5ms）：2.5
     *   帧结构3（2.5ms双）：5
     *   帧结构4（5ms）：5
     *   帧结构5（1ms）：1
     *   FDD：10（在其它方法中直接写死）
     */
    private double getTDDFramePeriod(String frameType) {
        log.info("获取TDD帧结构周期，frameType={}", frameType);
        return switch (frameType) {
            case "FRAME1" -> 2.0;    // 帧结构1（2ms）
            case "FRAME2" -> 2.5;    // 帧结构2（2.5ms）
            case "FRAME3" -> 5.0;    // 帧结构3（2.5ms双）
            case "FRAME4" -> 5.0;    // 帧结构4（5ms）
            case "FRAME5" -> 1.0;    // 帧结构5（1ms）
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
            case "prbNum" -> data.getPrbNum(); // 使用新的字段名
            case "bler" -> correctBlerValue(data); // 使用修正方法获取BLER值
            default -> throw new IllegalArgumentException("未知的维度: " + dimension);
        };
    }

    /**
     * 返回数据库中的BLER值
     * 确保返回正确的值以便前端显示
     */
    private Double correctBlerValue(TestData data) {
        // 直接返回数据库中的BLER值
        if (data.getBler() != null) {
            // 确保值非负且非零（避免显示为0.0000%）
            double blerValue = Math.max(0, data.getBler());
            // 如果BLER值非常接近零，返回一个小的非零值以确保可见
            if (blerValue < 0.0001 && blerValue > 0) {
                return 0.0001; // 设置最小值为0.0001%
            }
            return blerValue;
        }
        return null; // 如果没有有效值，则返回空
    }

    /**
     * 这些方法在现有系统中不再需要，直接使用TestData进行处理
     * 如果有调用这些方法的地方，返回空数据或直接使用TestData数据
     */
    @Cacheable(value = "timeSeriesData", key = "#fileName + '_' + #startTime + '_' + #endTime")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTimeSeriesDataCache(String fileName, Integer startTime, Integer endTime) {
        log.warn("调用了未实现的getTimeSeriesDataCache方法，返回空列表");
        return Collections.emptyList();
    }

    @Transactional(readOnly = true)
    public Stream<Map<String, Object>> getTimeSeriesDataStream(String fileName, Integer startTime, Integer endTime) {
        log.warn("调用了未实现的getTimeSeriesDataStream方法，返回空流");
        return Stream.empty();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRateDistribution(String fileName, Integer startTime, Integer endTime, Integer rangeCount) {
        try {
            // 使用TestData代替TimeSeriesData，直接获取MAC吞吐量数据
            List<Double> rates = testDataRepository.findAll().stream()
                .map(TestData::getMacThroughput)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            if (rates.isEmpty()) {
                return Map.of(
                    "distribution", Collections.emptyMap(),
                    "min", 0.0,
                    "max", 0.0,
                    "count", 0
                );
            }

            DoubleSummaryStatistics stats = rates.stream()
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();

            double min = stats.getMin();
            double max = stats.getMax();
            int actualRangeCount = rangeCount != null ? rangeCount : 10;
            double interval = (max - min) / actualRangeCount;

            // 使用TreeMap保持区间顺序
            Map<String, Integer> distribution = new TreeMap<>();
            for (int i = 0; i < actualRangeCount; i++) {
                double rangeStart = min + i * interval;
                double rangeEnd = rangeStart + interval;
                distribution.put(String.format("%.2f-%.2f", rangeStart, rangeEnd), 0);
            }

            // 优化计数过程
            for (Double rate : rates) {
                if (rate != null) {
                    int index = (int) ((rate - min) / interval);
                    if (index == actualRangeCount) index--;
                    String range = String.format("%.2f-%.2f",
                        min + index * interval,
                        min + (index + 1) * interval);
                    distribution.merge(range, 1, Integer::sum);
                }
            }

            return Map.of(
                "distribution", distribution,
                "min", min,
                "max", max,
                "count", rates.size()
            );
        } catch (Exception e) {
            log.error("计算速率分布失败", e);
            throw new RuntimeException("计算速率分布失败", e);
        }
    }

    /**
     * 计算理论速率并与MAC层速率对比
     * @param mode 计算模式（FDD/TDD）
     * @param frameType TDD模式下的帧结构类型
     * @param dlSlots 下行时隙数
     * @param specialSlots 特殊时隙数
     * @return 理论速率和MAC层实际速率的对比数据
     */
    @Transactional(readOnly = true)
    public Map<String, Object> calculateRateComparison(String mode, String frameType,
                                                     Integer dlSlots, Integer specialSlots) {
        try {
            log.info("开始速率对比分析: mode={}, frameType={}, dlSlots={}, specialSlots={}",
                    mode, frameType, dlSlots, specialSlots);

            // 获取测试数据，按时间升序排序
            List<TestData> allData = testDataRepository.findAll(Sort.by("testTime").ascending())
                    .stream()
                    .limit(2000) // 限制最多处理2000条数据
                    .collect(Collectors.toList());

            log.info("速率对比分析：从数据库获取到 {} 条数据记录", allData.size());

            if (allData.isEmpty()) {
                log.warn("没有找到测试数据，无法进行速率对比分析");
                return Map.of(
                    "timePoints", Collections.emptyList(),
                    "macRates", Collections.emptyList(),
                    "theoreticalRates", Collections.emptyList()
                );
            }

            // 获取周期值
            double period;
            if ("FDD".equals(mode)) {
                period = 10.0; // FDD模式固定为10ms
            } else {
                period = getTDDFramePeriod(frameType);
            }

            log.info("速率对比分析: 周期={}ms, 帧结构={}, 下行时隙={}, 特殊时隙={}", 
                period, frameType, dlSlots, specialSlots);

            // 为每个数据点计算理论速率
            List<String> timePoints = new ArrayList<>();
            List<Double> macRates = new ArrayList<>();
            List<Double> theoreticalRates = new ArrayList<>();
            
            int validPointCount = 0; // 统计有效数据点数量
            double maxMacRate = 0.0; // 记录最大MAC速率
            double maxTheoRate = 0.0; // 记录最大理论速率

            // 采样处理
            int samplingInterval = allData.size() > 600 ? allData.size() / 600 : 1;
            log.info("数据点总数: {}, 采样间隔: {}", allData.size(), samplingInterval);

            // 跟踪最后一次有效的参数值
            int lastValidModulationOrder = 4; // 默认16QAM
            int lastValidRank = 2; // 默认2层
            int lastValidPrb = 25; // 默认25个PRB

            for (int i = 0; i < allData.size(); i += samplingInterval) {
                TestData data = allData.get(i);
                
                // 获取MAC层速率
                Double macRate = data.getMacThroughput() != null ? data.getMacThroughput() : 0.0;
                
                // 更新最大MAC速率
                if (macRate > maxMacRate) {
                    maxMacRate = macRate;
                }
                
                // 获取当前数据点的实际参数
                Double mcs = data.getMcs();
                Integer rank = data.getRank();
                Integer prb = data.getPrbNum();
                
                // 日志记录原始数据
                log.debug("数据点 #{}: 时间={}, MCS={}, 层数={}, PRB={}, MAC速率={}",
                    i, data.getTestTime(), mcs, rank, prb, macRate);
                
                // 使用实际参数计算理论速率
                double theoreticalRate = 0.0;
                
                try {
                    // 从MCS映射获取调制阶数
                    int currModulationOrder = (mcs != null) ? mapMcsToModulationOrder(mcs) : lastValidModulationOrder;
                    
                    // 获取当前数据点的层数
                    int currRank = (rank != null && rank > 0) ? rank : lastValidRank;
                    
                    // 获取当前数据点的PRB数
                    int currPrb = (prb != null && prb > 0) ? prb : lastValidPrb;
                    
                    // 更新最后一次有效的参数值
                    if (mcs != null) lastValidModulationOrder = currModulationOrder;
                    if (rank != null && rank > 0) lastValidRank = currRank;
                    if (prb != null && prb > 0) lastValidPrb = currPrb;
                    
                    // 使用实际参数计算理论速率
                    theoreticalRate = calculateComparisonRate(
                        currModulationOrder, currRank, currPrb,
                        mode, frameType, dlSlots, specialSlots, period);
                    
                    validPointCount++;
                    
                    // 更新最大理论速率
                    if (theoreticalRate > maxTheoRate) {
                        maxTheoRate = theoreticalRate;
                    }
                    
                    // 记录理论值与实际MAC层速率的比较结果
                    if (theoreticalRate < macRate && macRate > 0) {
                        log.warn("数据点 #{}: 理论值({})低于MAC实际值({}), 差值: {} Mbps", 
                               i, theoreticalRate, macRate, (macRate - theoreticalRate));
                    }
                        
                } catch (Exception e) {
                    log.error("计算理论速率出错: {}", e.getMessage());
                    theoreticalRate = 0.0;
                }
                
                // 添加到结果集
                timePoints.add(data.getTestTime().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")));
                macRates.add(macRate);
                theoreticalRates.add(theoreticalRate);
            }
            
            // 确保包含最后一个点
            if (!allData.isEmpty() && (allData.size() - 1) % samplingInterval != 0) {
                TestData lastData = allData.get(allData.size() - 1);
                
                Double lastMacRate = lastData.getMacThroughput() != null ? lastData.getMacThroughput() : 0.0;
                Double lastMcs = lastData.getMcs();
                Integer lastRank = lastData.getRank();
                Integer lastPrb = lastData.getPrbNum();
                
                log.debug("处理最后一个数据点: 时间={}, MCS={}, 层数={}, PRB={}, MAC速率={}",
                    lastData.getTestTime(), lastMcs, lastRank, lastPrb, lastMacRate);
                
                double lastTheoreticalRate = 0.0;
                try {
                    // 使用最后一个数据点的实际参数
                    int currModulationOrder = (lastMcs != null) ? mapMcsToModulationOrder(lastMcs) : lastValidModulationOrder;
                    int currRank = (lastRank != null && lastRank > 0) ? lastRank : lastValidRank;
                    int currPrb = (lastPrb != null && lastPrb > 0) ? lastPrb : lastValidPrb;
                    
                    lastTheoreticalRate = calculateComparisonRate(
                        currModulationOrder, currRank, currPrb,
                        mode, frameType, dlSlots, specialSlots, period);
                        
                    if (lastTheoreticalRate < lastMacRate && lastMacRate > 0) {
                        log.warn("最后数据点: 理论值({})低于MAC实际值({}), 差值: {} Mbps", 
                               lastTheoreticalRate, lastMacRate, (lastMacRate - lastTheoreticalRate));
                    }
                } catch (Exception e) {
                    log.error("计算最后一个点理论速率出错: {}", e.getMessage());
                }
                
                timePoints.add(lastData.getTestTime().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")));
                macRates.add(lastMacRate);
                theoreticalRates.add(lastTheoreticalRate);
            }
            
            // 如果所有理论速率都是0，记录警告
            if (theoreticalRates.stream().allMatch(rate -> rate == 0.0)) {
                log.warn("所有计算的理论速率为0，可能是参数设置或测量数据有问题");
            }
            
            log.info("速率对比分析完成，共处理{}个有效数据点，有效计算点{}个", timePoints.size(), validPointCount);
            log.info("MAC速率范围: {}到{} Mbps", 
                    macRates.stream().mapToDouble(Double::doubleValue).min().orElse(0),
                    macRates.stream().mapToDouble(Double::doubleValue).max().orElse(0));
            log.info("理论速率范围: {}到{} Mbps", 
                    theoreticalRates.stream().mapToDouble(Double::doubleValue).min().orElse(0),
                    theoreticalRates.stream().mapToDouble(Double::doubleValue).max().orElse(0));

            // 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("timePoints", timePoints);
            result.put("macRates", macRates);
            result.put("theoreticalRates", theoreticalRates);
            result.put("parameters", Map.of(
                "mode", mode,
                "frameType", frameType != null ? frameType : "",
                "dlSlots", dlSlots,
                "specialSlots", specialSlots != null ? specialSlots : 0
            ));

            return result;
        } catch (Exception e) {
            log.error("速率对比分析失败", e);
            throw new RuntimeException("速率对比分析失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 速率对比分析专用的理论速率计算方法
     * 注意：此方法专门为速率对比分析设计，与峰值速率计算模块分开
     */
    private double calculateComparisonRate(int modulationOrder, int dlStreams, int prb,
                                           String mode, String frameType,
                                           int dlSlots, Integer specialSlots, double period) {
        try {
            log.info("速率对比分析 - 理论速率参数: modulationOrder={}, dlStreams={}, prb={}, mode={}, frameType={}, dlSlots={}, specialSlots={}, period={}",
                     modulationOrder, dlStreams, prb, mode, frameType, dlSlots, specialSlots, period);
            
            // 参数校验 - 使用原始值，不做任何修改
            if (modulationOrder <= 0 || dlStreams <= 0 || prb <= 0 || dlSlots <= 0) {
                log.warn("速率对比分析 - 参数存在非法值: modulationOrder={}, dlStreams={}, prb={}, dlSlots={}",
                         modulationOrder, dlStreams, prb, dlSlots);
                return 0.0; // 参数无效返回0.0
            }

            // --- 理论速率计算 - 严格按照标准公式 ---
            final double CODING_EFFICIENCY = 0.925; // 编码效率近似值
            int RE = 12 * (14 - 1); // 每个PRB每个时隙的RE数（考虑1个符号的开销）
            
            // 计算Nifo = RE * PRB * 调制阶数 * DL流数 * 编码效率
            double Nifo = Math.floor(RE * prb * modulationOrder * dlStreams * CODING_EFFICIENCY);
            
            // Nifo01 计算逻辑 (更紧凑形式)
            // 1. 计算 n = int(log2(Nifo - 24)) - 5
            double n = Math.floor(Math.log(Nifo - 24.0) / Math.log(2.0)) - 5.0;
            
            // 2. 计算 2^n 
            double powern = Math.pow(2, n);
            
            // 3. 计算用于 MAX 比较的项: (2^n) * int((Nifo - 24) / (2^n))
            double term_for_max;
            if (powern == 0.0) { // 保持对 2^n == 0 的检查
                term_for_max = 0.0;
            } else {
                term_for_max = powern * Math.floor((Nifo - 24.0) / powern);
            }
            
            // 4. Nifo01 = MAX(3840, term_for_max)
            double Nifo01 = Math.max(3840.0, term_for_max);
            
            // 计算TBS = 8 * 调制阶数 * floor((Nifo01 + 24)/(8 * 调制阶数))
            double TBS = 8 * modulationOrder * Math.floor((Nifo01 + 24) / (8 * modulationOrder));
            // log.debug("速率对比分析 - 计算TBS: Nifo01={}, modulationOrder={}, TBS={}",
            //        Nifo01, modulationOrder, TBS); // Simplified log below

            // DETAILED LOGGING FOR TBS CALCULATION INPUTS AND OUTPUT
            log.info("calculateComparisonRate IN: modOrder={}, dlStreams={}, prb={}, mode={}, frameType={}, dlSlots={}, spSlots={}, period={}. INTERMEDIATE: Nifo={}, Nifo01={}, TBS={}",
                     modulationOrder, dlStreams, prb, mode, frameType, dlSlots, specialSlots, period, Nifo, Nifo01, TBS);
            
            // 计算每秒时隙数
            double slotsPerSecond;
            
            if ("FDD".equals(mode)) {
                // FDD模式: 固定10ms周期
                slotsPerSecond = dlSlots * (1000.0 / 10.0);
                log.debug("速率对比分析 - FDD计算: 周期=10ms, 下行时隙={}, 每秒时隙数=dlSlots*(1000/10)={}",
                        dlSlots, slotsPerSecond);
            } else { // TDD模式
                int specialSlotsValue = (specialSlots != null && specialSlots >= 0) ? specialSlots : 0;
                
                // 帧结构5的特殊处理，与峰值速率计算保持一致
                if ("FRAME5".equals(frameType) && specialSlotsValue == 1) {
                    slotsPerSecond = dlSlots * (1000.0 / period);
                    log.debug("速率对比分析 - FRAME5特殊处理: 周期={}ms, 下行时隙={}, 特殊时隙=1, 计算公式=dlSlots*(1000/period)={}",
                            period, dlSlots, slotsPerSecond);
                } else {
                    // 常规TDD计算
                    slotsPerSecond = (dlSlots + specialSlotsValue) * (1000.0 / period);
                    log.debug("速率对比分析 - TDD计算: 周期={}ms, 下行时隙={}, 特殊时隙={}, 有效时隙={}, 计算公式=(dlSlots+specialSlots)*(1000/period)={}",
                            period, dlSlots, specialSlotsValue, (dlSlots + specialSlotsValue), slotsPerSecond);
                }
            }

            // 最终理论速率计算 (Mbps) = (TBS * 每秒时隙数) / 10^6
            double theoreticalRate = (TBS * slotsPerSecond) / 1_000_000.0;
            log.info("速率对比分析 - 最终理论速率计算: TBS={}, 每秒时隙数={}, 理论速率=(TBS*slotsPerSecond)/10^6={} Mbps",
                    TBS, slotsPerSecond, theoreticalRate);
            
            return theoreticalRate;
        } catch (Exception e) {
            log.error("速率对比分析 - 理论速率计算失败: {}", e.getMessage(), e);
            return 0.0; // 计算失败返回0
        }
    }


    /**
     * 将MCS值映射为调制阶数
     * 根据5G NR标准，MCS索引与调制方式的映射关系
     */
    private int mapMcsToModulationOrder(double mcsValue) {
        // 将double类型的MCS值转换为整数索引进行比较
        int mcsIndex = (int) Math.round(mcsValue);

        log.debug("MCS映射: 原始值={}, 四舍五入后索引={}", mcsValue, mcsIndex);

        if (mcsIndex >= 0 && mcsIndex <= 9) {
            return 2; // QPSK (调制阶数 Q_m = 2)
        } else if (mcsIndex >= 10 && mcsIndex <= 16) {
            return 4; // 16QAM (调制阶数 Q_m = 4)
        } else if (mcsIndex >= 17 && mcsIndex <= 23) {
            return 6; // 64QAM (调制阶数 Q_m = 6)
        } else if (mcsIndex >= 24 && mcsIndex <= 28) {
            return 8; // 256QAM (调制阶数 Q_m = 8)
        } else {
            // 处理超出定义范围的MCS索引
            log.warn("未知或不支持的MCS索引: {} (原始值: {}), 将使用默认调制阶数 2 (QPSK)", mcsIndex, mcsValue);
            return 2; // 默认返回 QPSK对应的调制阶数
        }
    }
}