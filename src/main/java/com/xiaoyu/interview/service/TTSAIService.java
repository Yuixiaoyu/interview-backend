package com.xiaoyu.interview.service;

import com.alibaba.dashscope.exception.NoApiKeyException;

/**
 * ClassName: TTSAIService
 * Description:
 *
 * @Author: fy
 * @create: 2025-08-10 18:25
 * @version: 1.0
 */
public interface TTSAIService {


    void tts(String text) throws NoApiKeyException;
}
