package com.xiaoyu.interview.demoInvoke;

import dev.langchain4j.community.model.dashscope.QwenChatModel;

/**
 * ClassName: LangChainAiInvoke
 * Description:
 *
 * @Author: fy
 * @create: 2025-06-08 13:08
 * @version: 1.0
 */
public class LangChainAiInvoke {
    public static void main(String[] args) {
        QwenChatModel qwenChatModel = QwenChatModel.builder()
                .apiKey("")
                .build();
        String answer = qwenChatModel.chat("你好我是飞宇，现在正在开发AI智能面试项目");
        System.out.println(answer);
    }
}
