package com._5ganalysisrate.g5rate.dto;

import lombok.Data;
import java.util.List;
import lombok.AllArgsConstructor;

/**
 * 图表数据传输对象
 */
@Data
public class ChartDataDTO {
    /**
     * X轴数据（时间）
     */
    private List<String> xAxisData;
    
    /**
     * Y轴数据系列
     */
    private List<SeriesData> series;
    
    /**
     * 图表标题
     */
    private String title;
    
    /**
     * X轴标题
     */
    private String xAxisName;
    
    /**
     * Y轴标题
     */
    private String yAxisName;

    @Data
    public static class SeriesData {
        /**
         * 数据系列名称
         */
        private String name;
        
        /**
         * 数据值
         */
        private List<Double> data;
        
        /**
         * 图表类型（line, bar, pie等）
         */
        private String type;
        
        /**
         * 饼图数据
         */
        private List<PieData> pieData;
    }

    @Data
    @AllArgsConstructor
    public static class PieData {
        /**
         * 名称
         */
        private String name;
        
        /**
         * 值
         */
        private Double value;
    }
} 