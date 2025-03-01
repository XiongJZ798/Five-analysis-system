package com._5ganalysisrate.g5rate.controller;

import com._5ganalysisrate.g5rate.model.TestData;
import com._5ganalysisrate.g5rate.repository.TestDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.greaterThan;

@SpringBootTest
@AutoConfigureMockMvc
public class AnalysisControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestDataRepository testDataRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        testDataRepository.deleteAll();

        // 准备测试数据
        TestData data1 = new TestData();
        data1.setTestTime(LocalDateTime.now());
        data1.setRsrp(-85.0);
        data1.setSinr(15.0);
        data1.setMacThroughput(100.0);
        data1.setRank(2);
        data1.setMcs(15);
        data1.setRbNum(50);
        data1.setBler(1.5);

        TestData data2 = new TestData();
        data2.setTestTime(LocalDateTime.now().plusHours(1));
        data2.setRsrp(-90.0);
        data2.setSinr(12.0);
        data2.setMacThroughput(80.0);
        data2.setRank(1);
        data2.setMcs(12);
        data2.setRbNum(40);
        data2.setBler(2.0);

        testDataRepository.saveAll(Arrays.asList(data1, data2));
    }

    @Test
    void testGetTimeSeriesData() throws Exception {
        Map<String, List<String>> request = new HashMap<>();
        request.put("dimensions", Arrays.asList("rsrp", "sinr"));

        mockMvc.perform(post("/analysis/time-series-chart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.series").isArray())
                .andExpect(jsonPath("$.data.series.length()").value(2));
    }

    @Test
    void testGetRateDistribution() throws Exception {
        mockMvc.perform(post("/analysis/rate-distribution-chart")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.pieData").isArray());
    }

    @Test
    void testCalculatePeakRate_FDD() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("mode", "FDD");
        request.put("bandwidth", 100.0);
        request.put("dlSlots", 10);

        mockMvc.perform(post("/analysis/calculate-peak-rate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber())
                .andExpect(jsonPath("$.data", greaterThan(0.0)));
    }

    @Test
    void testCalculatePeakRate_TDD() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("mode", "TDD");
        request.put("frameType", "FRAME1");
        request.put("bandwidth", 100.0);
        request.put("dlSlots", 8);
        request.put("specialSlots", 2);

        mockMvc.perform(post("/analysis/calculate-peak-rate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber())
                .andExpect(jsonPath("$.data", greaterThan(0.0)));
    }

    @Test
    void testCalculatePeakRate_InvalidParameters() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("mode", "INVALID");
        request.put("bandwidth", 100.0);
        request.put("dlSlots", 10);

        mockMvc.perform(post("/analysis/calculate-peak-rate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }
} 