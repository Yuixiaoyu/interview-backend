package com.xiaoyu.interview.ai;

import cn.hutool.core.lang.UUID;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeSpeechSynthesisOptions;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisModel;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisPrompt;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisResponse;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.xiaoyu.interview.common.ErrorCode;
import com.xiaoyu.interview.config.DashScopeConfig;
import com.xiaoyu.interview.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * ClassName: SpeechModel
 * Description:
 * TTS模型
 *
 * @Author: fy
 * @create: 2025-08-26 5:19
 * @version: 1.0
 */
@Component
@Slf4j
public class SpeechModel {

    @Resource
    private SpeechSynthesisModel speechSynthesisModel;
    @Resource
    private DashScopeConfig dashScopeConfig;

    public ByteBuffer tts(String message) {
        // 请求参数
        SpeechSynthesisParam param =
                SpeechSynthesisParam.builder()
                        // 若没有将API Key配置到环境变量中，需将下面这行代码注释放开，并将your-api-key替换为自己的API Key
                         .apiKey(dashScopeConfig.getApiKey())
                        .model("cosyvoice-v2") // 模型
                        .voice("longxiaochun_v2") // 音色
                        .build();

        // 同步模式：禁用回调（第二个参数为null）
        SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, null);
        // 阻塞直至音频返回
        ByteBuffer audio = synthesizer.call(message);

        return audio;

    }

}
