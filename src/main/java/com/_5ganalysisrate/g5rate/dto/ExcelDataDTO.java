package com._5ganalysisrate.g5rate.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Excel数据传输对象
 */
@Data
public class ExcelDataDTO {
    private LocalDateTime testTime;    // 测试时间
    private Double rsrp;               // RSRP值
    private Double sinr;               // SINR值
    private Double macThroughput;      // MAC层吞吐量
    private Integer rank;              // MIMO层数
    private Integer mcs;               // 调制编码方案
    private Integer rbNum;             // 资源块数
    private Double bler;               // 误块率
    
    // 用于文件上传响应
    private Integer importCount;       // 导入数据条数
    private String message;            // 处理结果信息
    private Boolean success;           // 是否成功
} 