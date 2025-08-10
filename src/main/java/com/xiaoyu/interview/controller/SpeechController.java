package com.xiaoyu.interview.controller;

import com.alibaba.cloud.ai.dashscope.audio.DashScopeSpeechSynthesisOptions;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisModel;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisPrompt;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisResponse;
import jakarta.annotation.Resource;
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
                .build();
        SpeechSynthesisResponse response = speechSynthesisModel.call(new SpeechSynthesisPrompt(TEXT,options));
        File file = new File(FILE_PATH + "/output.mp3");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            ByteBuffer byteBuffer = response.getResult().getOutput().getAudio();
            fos.write(byteBuffer.array());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new IOException(e.getMessage());
        }
    }
    @GetMapping("/ttsStreamFile")
    public void ttsStreamFile() {
        Flux<SpeechSynthesisResponse> response = speechSynthesisModel.stream(new SpeechSynthesisPrompt(TEXT));
        CountDownLatch latch = new CountDownLatch(1);
        File file = new File(FILE_PATH + "/output-stream.mp3");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            response.doFinally(signal -> latch.countDown())
                    .subscribe(synthesisResponse -> {
                ByteBuffer byteBuffer = synthesisResponse.getResult().getOutput().getAudio();
                byte[] bytes = new byte[byteBuffer.remaining()];
                byteBuffer.get(bytes);
                try {
                    fos.write(bytes);
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            latch.await();
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
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