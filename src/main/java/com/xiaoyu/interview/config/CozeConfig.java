package com.xiaoyu.interview.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

// CozeConfig.java 配置类
@Configuration
public class CozeConfig {
    @Value("${coze.clientId}")
    private String clientId;
    
    @Value("${coze.token}")
    private String token;

    @Bean
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("CozeSSE-");
        executor.initialize();
        return executor;
    }

    public String getClientId() {
        return clientId;
    }

    public String getToken() {
        return token;
    }
}
