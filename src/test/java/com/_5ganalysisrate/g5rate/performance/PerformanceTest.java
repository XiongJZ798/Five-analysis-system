package com._5ganalysisrate.g5rate.performance;

import com._5ganalysisrate.g5rate.model.TestData;
import com._5ganalysisrate.g5rate.repository.TestDataRepository;
import com._5ganalysisrate.g5rate.service.AnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PerformanceTest {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private TestDataRepository testDataRepository;

    @BeforeEach
    void setUp() {
        testDataRepository.deleteAll();
        // 生成大量测试数据
        List<TestData> testDataList = generateTestData(1000);
        testDataRepository.saveAll(testDataList);
    }

    @Test
    void testTimeSeriesDataPerformance() {
        // 测试时序数据分析性能
        long startTime = System.nanoTime();

        List<String> dimensions = Arrays.asList("rsrp", "sinr", "macThroughput");
        Map<String, Object> result = analysisService.getTimeSeriesData(dimensions);

        long endTime = System.nanoTime();
        long duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.containsKey("series"));
        
        // 性能断言：处理时间应该小于1秒
        assertTrue(duration < 1000, 
            "Time series data analysis took too long: " + duration + "ms");
    }

    @Test
    void testRateDistributionPerformance() {
        // 测试速率分布分析性能
        long startTime = System.nanoTime();

        Map<String, Object> result = analysisService.getRateDistribution(null);

        long endTime = System.nanoTime();
        long duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.containsKey("pieData"));
        
        // 性能断言：处理时间应该小于500毫秒
        assertTrue(duration < 500, 
            "Rate distribution analysis took too long: " + duration + "ms");
    }

    @Test
    void testPeakRateCalculationPerformance() {
        // 测试峰值速率计算性能
        long startTime = System.nanoTime();

        // 执行100次计算以测试性能
        for (int i = 0; i < 100; i++) {
            double result = analysisService.calculatePeakRate(
                "FDD", null, 100.0, 10, null);
            assertTrue(result > 0);
        }

        long endTime = System.nanoTime();
        long duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        // 性能断言：100次计算应该在200毫秒内完成
        assertTrue(duration < 200, 
            "Peak rate calculations took too long: " + duration + "ms");
    }

    @Test
    void testDatabaseQueryPerformance() {
        // 测试数据库查询性能
        long startTime = System.nanoTime();

        List<TestData> allData = testDataRepository.findAllByOrderByTestTimeAsc();

        long endTime = System.nanoTime();
        long duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        // 验证结果
        assertFalse(allData.isEmpty());
        
        // 性能断言：查询时间应该小于300毫秒
        assertTrue(duration < 300, 
            "Database query took too long: " + duration + "ms");
    }

    private List<TestData> generateTestData(int count) {
        List<TestData> dataList = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now();

        for (int i = 0; i < count; i++) {
            TestData data = new TestData();
            data.setTestTime(baseTime.plusSeconds(i));
            data.setRsrp(-85.0 - Math.random() * 10);
            data.setSinr(15.0 + Math.random() * 5);
            data.setMacThroughput(100.0 + Math.random() * 50);
            data.setRank(1 + (int)(Math.random() * 4));
            data.setMcs(10 + (int)(Math.random() * 15));
            data.setRbNum(40 + (int)(Math.random() * 20));
            data.setBler(1.0 + Math.random() * 2);
            dataList.add(data);
        }

        return dataList;
    }
} 