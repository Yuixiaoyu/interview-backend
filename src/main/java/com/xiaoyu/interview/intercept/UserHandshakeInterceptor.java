package com.xiaoyu.interview.intercept;

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
        if (request instanceof ServletServerHttpRequest) {

            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

            // 假设用户 id 存在 HttpSession 里
            Object userObj = servletRequest.getSession().getAttribute(USER_LOGIN_STATE);
            log.info("userObj: {}", (User) userObj);
            // 或者你用登录认证的 Principal
            // String userId = servletRequest.getUserPrincipal().getName();
            if (userObj != null) {
                attributes.put("user", (User) userObj);
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // 握手完成后不需要处理
    }
}
