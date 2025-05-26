package com._5ganalysisrate.g5rate.model.request;

import lombok.Data;

//计算模块请求参数
@Data
public class PeakRateRequest {
    // 带宽(MHz)
    private int bandwidth;
    // MIMO层数
    private int mimoLayers;
    // 调制阶数
    private int modulationOrder;
    // 编码率(%)
    private double codingRate;
} 