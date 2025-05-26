package com._5ganalysisrate.g5rate.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

/**
 * 实体类扫描配置
 * 明确指定要扫描的实体类，避免加载不存在的类
 */
@Configuration
@EntityScan(basePackages = {
    "com._5ganalysisrate.g5rate.model"
})
public class EntityExclusionConfig {
    // 配置通过注解完成
} 