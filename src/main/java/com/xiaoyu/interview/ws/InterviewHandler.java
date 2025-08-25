package com.xiaoyu.interview.ws;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoyu.interview.constant.RedisConstant;
import com.xiaoyu.interview.model.entity.AnswerPayload;
import com.xiaoyu.interview.model.entity.User;
import com.xiaoyu.interview.service.UserService;
import com.xiaoyu.interview.utils.RedisUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.xiaoyu.interview.constant.UserConstant.USER_LOGIN_STATE;

/**
 * ClassName: InterviewHandler
 * Description:
 *
 * @Author: fy
 * @create: 2025-08-18 21:28
 * @version: 1.0
 */
@Slf4j
@Component
public class InterviewHandler implements WebSocketHandler {

    @Resource
    private RedisUtil redisUtil;

    // 题库
    private static List<String> questions = List.of(
            "请介绍一次高并发场景下的优化经历",
            "Redis 缓存穿透如何解决？",
            "Spring 事务失效的常见原因？"
    );

    // session -> 当前题号
    private final Map<WebSocketSession, Integer> cursor = new ConcurrentHashMap<>();

    // 握手完成
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("🤝 握手成功: {}", session.getId());
        //拿到当前登录用户id
        Object userIdObj = session.getAttributes().get("loginId");
        //这里必须先转成string，否则会报类型转换错误
        String userIdStr = (String) userIdObj;
        long userId = Long.parseLong(userIdStr);
        log.info("用户id: {}", userId);
        //将题目信息存放到内存中
        questions= redisUtil.getList(RedisConstant.USER_QUESTION_REDIS_KEY_PREFIX + userId);

        cursor.put(session, 0);
        sendQuestion(session, 0);
    }

    // 收到答案
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String payload = (String) message.getPayload();
        AnswerPayload ans = new ObjectMapper().readValue(payload, AnswerPayload.class);

        String fb = "回答的很好";
        session.sendMessage(new TextMessage(new ObjectMapper().writeValueAsString(fb)));

        int next = ans.getSeq() + 1;
        if (next < questions.size()) {
            cursor.put(session, next);
            sendQuestion(session, next);
        } else {
            session.sendMessage(new TextMessage("{\"type\":\"DONE\",\"msg\":\"面试结束\"}"));
            session.close(CloseStatus.NORMAL);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误: {}", session.getId(), exception);
        cursor.remove(session);
        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (IOException e) {
            log.error("关闭WebSocket连接失败: {}", session.getId(), e);
        }
    }


    private void sendQuestion(WebSocketSession session, int seq) throws IOException {
        String json = new ObjectMapper()
                .writeValueAsString(Map.of("type","QUESTION","seq",seq,"content",questions.get(seq)));
        session.sendMessage(new TextMessage(json));
    }

    // 关闭清理
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        cursor.remove(session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }


}
