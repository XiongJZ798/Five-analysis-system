package com._5ganalysisrate.g5rate.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 时序数据查询参数
 */
@Data
public class TimeSeriesQueryDTO {
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 需要查询的维度列表
     * 可选值：rsrp, sinr, macThroughput, rank, mcs, rbNum, bler
     */
    private List<String> dimensions;
} 