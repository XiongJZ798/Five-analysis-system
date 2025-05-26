package com._5ganalysisrate.g5rate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Value("${security.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;
    
    @Value("${security.cors.allowed-origin-patterns:*}")
    private String allowedOriginPatterns;

    @Value("${security.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${security.cors.allowed-headers:Authorization,Content-Type}")
    private String allowedHeaders;

    @Value("${security.cors.max-age:3600}")
    private long maxAge;

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // 配置允许的源
        if (allowedOriginPatterns != null && !allowedOriginPatterns.isEmpty()) {
            // 优先使用pattern以支持通配符
            Arrays.stream(allowedOriginPatterns.split(","))
                  .forEach(pattern -> config.addAllowedOriginPattern(pattern.trim()));
        } else if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            // 回退到精确匹配的源
            Arrays.stream(allowedOrigins.split(","))
                  .forEach(origin -> config.addAllowedOrigin(origin.trim()));
        } else {
            // 默认只允许localhost:3000
            config.addAllowedOrigin("http://localhost:3000");
        }
        
        // 配置允许的HTTP方法
        if (allowedMethods != null) {
            Arrays.stream(allowedMethods.split(","))
                  .forEach(method -> config.addAllowedMethod(method.trim()));
        } else {
            config.addAllowedMethod("*");
        }
        
        // 配置允许的HTTP头
        if (allowedHeaders != null) {
            Arrays.stream(allowedHeaders.split(","))
                  .forEach(header -> config.addAllowedHeader(header.trim()));
        } else {
            config.addAllowedHeader("*");
        }
        
        config.setAllowCredentials(true);
        config.setMaxAge(maxAge);
        
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
} 