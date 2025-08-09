package com.xiaoyu.interview.demoInvoke;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * ClassName: SpringAIInvoke
 * Description: SpringAi 调用Ollama大模型gemma3:1b
 * @Author: fy
 * @create: 2025-06-08 11:31
 * @version: 1.0
 */
//@Component
public class OllamaAIInvoke implements CommandLineRunner {

    @Resource
    private ChatModel ollamaChatModel;

    @Override
    public void run(String... args) throws Exception {
        AssistantMessage assistantMessage = ollamaChatModel.call(new Prompt("你好呀,我是飞宇，正在开发AI面试刷题平台"))
                .getResult()
                .getOutput();
        System.out.println(assistantMessage.getText());
    }
}
