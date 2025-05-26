package com._5ganalysisrate.g5rate.config;

import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * JPA配置
 * 自定义Hibernate的行为，禁用架构验证
 */
@Configuration
public class JpaConfig {

    /**
     * 自定义Hibernate配置，禁用架构验证
     * 这样系统启动时不会因为缺少表而失败
     */
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return hibernateProperties -> 
            hibernateProperties.put("hibernate.hbm2ddl.auto", "none");
    }
} 