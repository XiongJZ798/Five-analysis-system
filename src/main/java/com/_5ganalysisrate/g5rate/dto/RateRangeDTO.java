package com._5ganalysisrate.g5rate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//RateRangeDTO用于表示速率范围
@Data
@NoArgsConstructor // 提供无参构造函数
@AllArgsConstructor // 提供全参构造函数
public class RateRangeDTO {
    private Double min; // 范围的最小值，使用Double类型允许null值
    private Double max; // 范围的最大值，使用Double类型允许null值
}
