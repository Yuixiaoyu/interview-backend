package com.xiaoyu.interview.config;

import com.xiaoyu.interview.intercept.UserHandshakeInterceptor;
import com.xiaoyu.interview.ws.ASRWebSocketHandler;
import com.xiaoyu.interview.ws.InterviewHandler;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    private InterviewHandler interviewHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new ASRWebSocketHandler(), "/asr")
                .setAllowedOrigins("*");
        // 新增第二个
        registry.addHandler(interviewHandler, "/interview")
                .addInterceptors(new UserHandshakeInterceptor())
                .setAllowedOrigins("*");
    }
}