package com.xiaoyu.interview.demoInvoke;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * ClassName: SpringAIInvoke
 * Description: SpringAi 调用ali大模型
 * @Author: fy
 * @create: 2025-06-08 11:31
 * @version: 1.0
 */
//@Component
public class SpringAIInvoke implements CommandLineRunner {

    @Resource
    private ChatModel dashscopeChatModel;

    @Override
    public void run(String... args) throws Exception {
        AssistantMessage assistantMessage = dashscopeChatModel.call(new Prompt("你好呀"))
                .getResult()
                .getOutput();
        System.out.println(assistantMessage.getText());
    }
}
