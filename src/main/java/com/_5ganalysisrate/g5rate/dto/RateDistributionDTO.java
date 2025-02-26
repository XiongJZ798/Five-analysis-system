package com._5ganalysisrate.g5rate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 速率分布统计结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RateDistributionDTO {
    /**
     * 区间名称
     */
    private String rangeName;
    
    /**
     * 百分比
     */
    private double percentage;
} 