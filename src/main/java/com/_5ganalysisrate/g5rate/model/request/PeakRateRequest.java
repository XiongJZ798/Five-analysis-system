package com._5ganalysisrate.g5rate.model.request;

import lombok.Data;

//计算模块请求参数
@Data
public class PeakRateRequest {
    private int bandwidth;        // 带宽(MHz)
    private int mimoLayers;       // MIMO层数
    private int modulationOrder;  // 调制阶数
    private double codingRate;    // 编码率(%)
} 