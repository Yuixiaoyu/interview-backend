package com.xiaoyu.interview.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author: xiaoyu
 * @date: 2025/8/23 14:05
 */
@Component
@Data
public class DashScopeConfig {
    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;
}
