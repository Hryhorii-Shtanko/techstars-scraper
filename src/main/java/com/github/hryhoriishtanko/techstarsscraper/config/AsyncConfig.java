package com.github.hryhoriishtanko.techstarsscraper.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Value("${app.executor.core-pool-size}")
    private int corePoolSize;

    @Value("${app.executor.max-pool-size}")
    private int maxPoolSize;

    @Value("${app.executor.queue-capacity}")
    private int queueCapacity;

    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("JobScraper-");
        executor.initialize();
        return executor;
    }
}