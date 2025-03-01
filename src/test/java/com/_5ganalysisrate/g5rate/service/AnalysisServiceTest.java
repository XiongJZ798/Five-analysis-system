package com._5ganalysisrate.g5rate.service;

import com._5ganalysisrate.g5rate.model.TestData;
import com._5ganalysisrate.g5rate.repository.TestDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AnalysisServiceTest {

    @Mock
    private TestDataRepository testDataRepository;

    @InjectMocks
    private AnalysisService analysisService;

    private TestData testData1;
    private TestData testData2;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testData1 = new TestData();
        testData1.setTestTime(LocalDateTime.now());
        testData1.setRsrp(-85.0);
        testData1.setSinr(15.0);
        testData1.setMacThroughput(100.0);
        testData1.setRank(2);
        testData1.setMcs(15);
        testData1.setRbNum(50);
        testData1.setBler(1.5);

        testData2 = new TestData();
        testData2.setTestTime(LocalDateTime.now().plusHours(1));
        testData2.setRsrp(-90.0);
        testData2.setSinr(12.0);
        testData2.setMacThroughput(80.0);
        testData2.setRank(1);
        testData2.setMcs(12);
        testData2.setRbNum(40);
        testData2.setBler(2.0);
    }

   // 时序数据分析测试
    @Test
    void testGetTimeSeriesData() {
        // 模拟数据库返回测试数据
        when(testDataRepository.findAllByOrderByTestTimeAsc())
            .thenReturn(Arrays.asList(testData1, testData2));

        // 测试多维度分析（RSRP和SINR）
        List<String> dimensions = Arrays.asList("rsrp", "sinr");
        Map<String, Object> result = analysisService.getTimeSeriesData(dimensions);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.containsKey("series"));
        assertTrue(result.containsKey("legend"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> series = (List<Map<String, Object>>) result.get("series");
        assertEquals(2, series.size());
    }

    //速率分布分析测试
    @Test
    void testGetRateDistribution() {
        // 准备测试数据
        when(testDataRepository.findAll())
            .thenReturn(Arrays.asList(testData1, testData2));

        // 执行测试
        Map<String, Object> result = analysisService.getRateDistribution(null);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.containsKey("pieData"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pieData = (List<Map<String, Object>>) result.get("pieData");
        assertFalse(pieData.isEmpty());
    }

    //FDD模式峰值速率计算测试
    @Test
    void testCalculatePeakRate_FDD() {
        // 执行测试
        double result = analysisService.calculatePeakRate(
            "FDD", null, 100.0, 10, null);

        // 验证结果
        assertTrue(result > 0);
    }

    //TDD模式峰值速率计算测试
    @Test
    void testCalculatePeakRate_TDD() {
        // 执行测试
        double result = analysisService.calculatePeakRate(
            "TDD", "FRAME1", 100.0, 8, 2);

        // 验证结果
        assertTrue(result > 0);
    }

    //无效模式异常测试
    @Test
    void testCalculatePeakRate_InvalidMode() {
        // 验证异常情况
        assertThrows(IllegalArgumentException.class, () ->
            analysisService.calculatePeakRate("INVALID", null, 100.0, 10, null));
    }

    @Test
    void testCalculatePeakRate_InvalidParameters() {
        // 验证参数验证
        assertThrows(IllegalArgumentException.class, () ->
            analysisService.calculatePeakRate("FDD", null, -100.0, 10, null));

        assertThrows(IllegalArgumentException.class, () ->
            analysisService.calculatePeakRate("TDD", "FRAME1", 100.0, -1, 2));

        assertThrows(IllegalArgumentException.class, () ->
            analysisService.calculatePeakRate("TDD", "FRAME1", 100.0, 8, 15));
    }
} 