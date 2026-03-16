package com._5ganalysisrate.g5rate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务线程池配置
 * 为 AI 分析流水线提供专用线程池
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${ai.async.core-pool-size:5}")
    private int corePoolSize;

    @Value("${ai.async.max-pool-size:20}")
    private int maxPoolSize;

    @Value("${ai.async.queue-capacity:100}")
    private int queueCapacity;

    @Bean(name = "analysisTaskExecutor")
    public Executor analysisTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("analysis-ai-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
