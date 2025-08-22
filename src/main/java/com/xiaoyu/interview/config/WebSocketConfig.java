package com.xiaoyu.interview.config;

import com.xiaoyu.interview.ws.ASRWebSocketHandler;
import com.xiaoyu.interview.ws.InterviewHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new ASRWebSocketHandler(), "/asr")
                .setAllowedOrigins("*");
        // 新增第二个
        registry.addHandler(new InterviewHandler(), "/interview")
                .setAllowedOrigins("*");
    }
}