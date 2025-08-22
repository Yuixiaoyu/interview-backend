package com.xiaoyu.interview.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoyu.interview.model.entity.AnswerPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ClassName: InterviewHandler
 * Description:
 *
 * @Author: fy
 * @create: 2025-08-18 21:28
 * @version: 1.0
 */
@Slf4j
public class InterviewHandler implements WebSocketHandler {

    // 题库
    private final List<String> questions = List.of(
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
