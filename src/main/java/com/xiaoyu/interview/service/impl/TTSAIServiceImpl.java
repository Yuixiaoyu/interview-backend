package com.xiaoyu.interview.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.xiaoyu.interview.common.ErrorCode;
import com.xiaoyu.interview.exception.BusinessException;
import com.xiaoyu.interview.service.TTSAIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * ClassName: TTSAIServiceImpl
 * Description:
 *
 * @Author: fy
 * @create: 2025-08-10 18:26
 * @version: 1.0
 */
@Service
@Slf4j
public class TTSAIServiceImpl implements TTSAIService {

    private static String model = "cosyvoice-v2"; // 模型
    private static String voice = "longxiaochun_v2"; // 音色

    private static final String FILE_PATH = "src/main/resources/tts";

    @Override
    public void tts(String text) throws NoApiKeyException {
        if (StrUtil.isBlank(text)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String uuid = UUID.randomUUID(false).toString();
        log.info("uuid:{}", uuid);
        File file = new File(FILE_PATH + "/"+uuid+".mp3");
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file.getPath()))) {
            // 配置语音合成参数
            SpeechSynthesisParam param =
                    SpeechSynthesisParam.builder()
                            // 若没有将API Key配置到环境变量中，需将下面这行代码注释放开，并将your-api-key替换为自己的API Key
                            .apiKey("your-key")
                            .model(model) // 模型
                            .voice(voice) // 音色
                            .build();

            SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, null);
            synthesizer.callAsFlowable(text).blockingForEach(result -> {
                ByteBuffer audioFrame = result.getAudioFrame();
                if (audioFrame != null) {
                    try {
                        Objects.requireNonNull(bos, "输出流不能为null");
                        Objects.requireNonNull(audioFrame, "缓冲区不能为null");

                        // 检查缓冲区是否有可读数据
                        if (audioFrame.hasRemaining()) {
                            // 获取缓冲区数据数组
                            byte[] bytes = new byte[audioFrame.remaining()];
                            audioFrame.get(bytes);
                            bos.write(bytes);
                        }
                        System.out.println(DateUtil.now() + " 写入音频帧 | 大小: " +
                                audioFrame.remaining() + " 字节");
                    } catch (IOException e) {
                        System.err.println("写入文件失败: " + e.getMessage());
                    }
                }
            });
            System.out.println(
                    "[Metric] requestId为：" + synthesizer.getLastRequestId() +
                            " | 首包延迟：" + synthesizer.getFirstPackageDelay() + "ms" +
                            " | 文件保存至: " + file.getPath()
            );
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
