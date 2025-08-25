package com.xiaoyu.interview.intercept;

import cn.dev33.satoken.stp.StpUtil;
import com.xiaoyu.interview.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

import static com.xiaoyu.interview.constant.UserConstant.USER_LOGIN_STATE;

@Slf4j
public class UserHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {
        // 1. 拿到原始 HttpServletRequest
        HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

        // 2. 从参数或 header 里取 token
        String token = servletRequest.getParameter("token");
        if (token == null) {
            token = servletRequest.getHeader("token"); // 也可以从 header 取
        }
        Object loginId = StpUtil.getLoginIdByToken(token);
        if (loginId != null) {
            attributes.put("loginId", loginId);
            return true;
        }
        return false; // 未登录直接拦截
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // 握手完成后不需要处理
    }
}
