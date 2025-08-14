package com.xiaoyu.interview.audio;

import com.alibaba.dashscope.exception.NoApiKeyException;
import com.xiaoyu.interview.service.TTSAIService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * ClassName: TestTTS
 * Description:
 *
 * @Author: fy
 * @create: 2025-08-11 18:59
 * @version: 1.0
 */
@SpringBootTest
public class TestTTS {

    @Resource
    TTSAIService ttsAIService;

    @Test
    public void testTTS(){
        try {
            ttsAIService.tts("你好求职者，请你先做一个自我介绍，简单讲述一下你的个人情况");
        } catch (NoApiKeyException e) {
            throw new RuntimeException(e);
        }
    }

}
