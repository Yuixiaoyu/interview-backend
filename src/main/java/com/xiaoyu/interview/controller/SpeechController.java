package com.xiaoyu.interview.controller;

import cn.hutool.core.lang.UUID;
import com.alibaba.cloud.ai.dashscope.api.DashScopeSpeechSynthesisApi;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeSpeechSynthesisOptions;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisModel;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisPrompt;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisResponse;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

/**
 * @desc 文本转语音（Text-To-Speech）
 * @date: 2025/3/11
 * @version: 1.0
 */
@Slf4j
@RestController
@RequestMapping("/ai/speech")
public class SpeechController{

    @Resource
    private SpeechSynthesisModel speechSynthesisModel;
    private static final String TEXT = "你好求职者，请你先做一个自我介绍，简单讲述一下你的个人情况";
    private static final String FILE_PATH = "src/main/resources/tts";

    @GetMapping("/tts")
    public void ttsFile() throws IOException {
        // 使用构建器模式创建 DashScopeSpeechSynthesisOptions 实例并设置参数
        DashScopeSpeechSynthesisOptions options = DashScopeSpeechSynthesisOptions.builder()
                .speed(1.0F)        // 设置语速
                .pitch(0.9)         // 设置音调
                .volume(75)         // 设置音量
                .voice("longxiaoxia")
                .build();
        SpeechSynthesisResponse response = speechSynthesisModel.call(new SpeechSynthesisPrompt(TEXT,options));
        String uuid = UUID.randomUUID(false).toString();
        log.info("uuid:{}", uuid);
        File file = new File(FILE_PATH + "/"+uuid+".mp3");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            ByteBuffer byteBuffer = response.getResult().getOutput().getAudio();
            fos.write(byteBuffer.array());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new IOException(e.getMessage());
        }
    }
    @GetMapping("/stream/tts")
    public String streamTTS(HttpServletResponse response, String userInputText) {
        if (StringUtils.isBlank(userInputText)) {
            userInputText = TEXT;
        }
        // 指定参数
        DashScopeSpeechSynthesisOptions speechSynthesisOptions = DashScopeSpeechSynthesisOptions.builder()
                .responseFormat(DashScopeSpeechSynthesisApi.ResponseFormat.MP3)
                .voice("longwan")
                //.withSpeed(1.0) // 语速，取值范围：0.5~2.0，默认值为 1.0
                .build();

        Flux<SpeechSynthesisResponse> speechSynthesisResponseFlux = speechSynthesisModel.stream(
                new SpeechSynthesisPrompt(userInputText, speechSynthesisOptions)
        );

        /**
         * 为什么使用 CountDownLatch？
         *  异步处理：Flux 是响应式编程的一部分，处理数据是异步的。这意味着数据的处理不会阻塞主线程。
         *  确保完成：CountDownLatch 用于等待所有异步操作完成。它允许主线程等待，直到所有数据处理完成后再继续执行
         */
        // 创建一个 CountDownLatch，初始计数为 1。
        CountDownLatch latch = new CountDownLatch(1);
        String outputFilePath =FILE_PATH+"/stream_tts_output.mp3";
        File file = new File(outputFilePath);
        try (FileOutputStream fos = new FileOutputStream(file)) {

            // 订阅 Flux<SpeechSynthesisResponse>
            speechSynthesisResponseFlux
                    .doFinally( // 在 Flux 所有数据处理完成后调用 countDown()
                            signal -> latch.countDown()
                    )
                    .subscribe(synthesisResponse -> {
                        // 处理每个 SpeechSynthesisResponse
                        ByteBuffer byteBuffer = synthesisResponse.getResult().getOutput().getAudio();
                        byte[] bytes = new byte[byteBuffer.remaining()];
                        byteBuffer.get(bytes);
                        try {
                            fos.write(bytes);
                        } catch (IOException e) {
                            log.error("调用 streamTTS 将字节数据写入文件异常 -->  e", e);
                            throw new RuntimeException(e);
                        }
                    }, error -> { // 处理错误
                        log.error(" streamTTS 调用异常 -->  error", error);
                        latch.countDown(); // 确保在发生错误时也减少计数器
                    });
            //
            // 主线程在这里等待所有异步操作完成，直到 CountDownLatch 的计数变为 0，即所有 SpeechSynthesisResponse 处理完成。
            latch.await();
        } catch (IOException | InterruptedException e) {
            log.error("调用 streamTTS 保存到文件异常 -->  userInputText ={}, e", userInputText, e);

            throw new RuntimeException(e);
        }
        return "SUCCESS";
    }

    /**
     * 将文字直接转成音频流
     * @return
     */
    @GetMapping(value = "/ttsStream", produces = "audio/mpeg")
    public Flux<byte[]> ttsStream() {
        return speechSynthesisModel.stream(new SpeechSynthesisPrompt(TEXT))
                .map(synthesisResponse -> {
                    ByteBuffer buffer = synthesisResponse.getResult().getOutput().getAudio();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    return bytes;
                });
    }
}