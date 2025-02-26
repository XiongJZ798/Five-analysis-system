package com._5ganalysisrate.g5rate.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 时序数据返回对象
 */
@Data
public class TimeSeriesDataDTO {
    /**
     * 时间点
     */
    private LocalDateTime timestamp;
    
    /**
     * 各维度数据
     * key: 维度名称
     * value: 维度值
     */
    private Map<String, Double> values;
} 