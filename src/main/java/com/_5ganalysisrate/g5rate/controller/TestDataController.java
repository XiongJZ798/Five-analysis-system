package com._5ganalysisrate.g5rate.controller;

import com._5ganalysisrate.g5rate.dto.ApiResponse;
import com._5ganalysisrate.g5rate.model.TestData;
import com._5ganalysisrate.g5rate.repository.TestDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试数据控制器
 */
@RestController
@RequestMapping("/testdata")
public class TestDataController {

    private static final Logger logger = LoggerFactory.getLogger(TestDataController.class);

    @Autowired
    private TestDataRepository testDataRepository;

    /**
     * 获取所有测试数据
     */
    @GetMapping
    public ApiResponse<?> getAllTestData(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        logger.info("获取测试数据，页码: {}, 每页大小: {}", page, size);
        
        try {
            // 简单实现，使用limit和offset
            List<TestData> dataList = testDataRepository.findAll();
            
            int total = dataList.size();
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, total);
            
            // 检查是否超出范围
            if (fromIndex >= total) {
                return ApiResponse.success(new HashMap<>());
            }
            
            // 获取分页数据
            List<TestData> pageData = dataList.subList(fromIndex, toIndex);
            
            // 构建分页响应
            Map<String, Object> response = new HashMap<>();
            response.put("data", pageData);
            response.put("total", total);
            response.put("page", page);
            response.put("size", size);
            response.put("pages", (int) Math.ceil((double) total / size));
            
            return ApiResponse.success(response);
        } catch (Exception e) {
            logger.error("获取测试数据失败", e);
            return ApiResponse.error(500, "获取数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据ID获取测试数据
     */
    @GetMapping("/{id}")
    public ApiResponse<?> getTestDataById(@PathVariable Long id) {
        logger.info("根据ID获取测试数据: {}", id);
        
        return testDataRepository.findById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "未找到ID为 " + id + " 的测试数据"));
    }
    
    /**
     * 删除所有测试数据
     */
    @DeleteMapping
    public ApiResponse<?> deleteAllTestData() {
        logger.info("删除所有测试数据");
        
        try {
            long count = testDataRepository.count();
            testDataRepository.deleteAll();
            
            Map<String, Object> response = new HashMap<>();
            response.put("deleted", count);
            return ApiResponse.success(response);
        } catch (Exception e) {
            logger.error("删除测试数据失败", e);
            return ApiResponse.error(500, "删除数据失败: " + e.getMessage());
        }
    }
} 