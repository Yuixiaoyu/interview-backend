package com.xiaoyu.interview.controller;


import cn.hutool.json.JSONUtil;
import com.xiaoyu.interview.config.StreamDataCallback;
import com.xiaoyu.interview.context.ResumeContext;
import com.xiaoyu.interview.service.impl.CozeService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ClassName: AnalyzeController
 * Description:
 * @Author: fy
 * @create: 2025-06-12 20:17
 * @version: 1.0
 */
@RestController
@RequestMapping("/analyze")
public class AnalyzeController {

    private static final Logger log = LoggerFactory.getLogger(AnalyzeController.class);

    @Resource
    private CozeService cozeService;

    // 创建线程池处理 SSE 事件
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();


    @GetMapping(value = "/fileStream",produces = "text/event-stream;charset=UTF-8")
    public SseEmitter streamAnalyzeFile(@RequestParam String fileUrl) {
        //创建sse
        SseEmitter sseEmitter = new SseEmitter(180_000L);

        StringBuilder stringBuilder = new StringBuilder();

        //异步处理调用防止影响主线程
        CompletableFuture.runAsync(()->{
            try {
                cozeService.analyzeFileStream(fileUrl, new StreamDataCallback() {
                    @Override
                    public void onData(String data) {
                        try {
                            String content = JSONUtil.parseObj(data).getStr("content");
                            log.info("onData:{}", data);
                            stringBuilder.append(content);
                            log.info("content:{}", content);
                            // 实时发送数据（事件名称：progress）
                            sseEmitter.send(SseEmitter.event()
                                    .name("progress")
                                    .data(data));
                        } catch (IOException e) {
                            sseEmitter.completeWithError(e);
                        }
                    }
                    @Override
                    public void onComplete() {
                        sseEmitter.complete(); // 流结束
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        sseEmitter.completeWithError(throwable);
                    }

                });
            } catch (Exception e) {
                log.info("发生异常：{}", e.getMessage());
                sseEmitter.completeWithError(e);
            }
        });

        sseEmitter.onCompletion(() -> {
            log.info("流结束");
            //将响应内容放入到threadLocal中
            ResumeContext.setResumeContextThreadLocal(stringBuilder.toString());
        });
        sseEmitter.onTimeout(() -> log.info("流超时"));
        sseEmitter.onError((e) -> log.info("发生错误：{}", e.getMessage()));
        return sseEmitter;
    }



    @GetMapping(value = "/videoStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analyzeVideo(@RequestParam String url) {
        // 创建 SSE 发射器，设置超时时间（60分钟）
        SseEmitter emitter = new SseEmitter(60 * 60 * 1000L);

        // 在独立线程中执行流处理
        sseExecutor.execute(() -> {
            try {
                WebClient client = WebClient.builder()
                        .baseUrl("http://localhost:5000")
                        .build();

                HashMap<String, String> hashMap = new HashMap<>();
                hashMap.put("video_url", url);

                // 发送初始事件
                emitter.send(SseEmitter.event()
                        .name("start")
                        .data("开始分析视频: " + url));

                // 获取流式响应
                Flux<String> stream = client.post()
                        .uri("/api/video/analyze_interview")
                        .header("Accept", "text/event-stream")
                        .bodyValue(hashMap)
                        .retrieve()
                        .bodyToFlux(String.class);

                // 订阅流事件
                stream.subscribe(
                        data -> {
                            log.info("收到数据：{}",data);
                            try {
                                emitter.send(data);
                                //// 发送数据事件
                                //emitter.send(SseEmitter.event()
                                //        .name("message")
                                //        .data(data));
                            } catch (IOException e) {
                                // 发送错误事件
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            try {
                                // 发送错误事件
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data(error.getMessage()));
                            } catch (IOException e) {
                                // 忽略二次错误
                            } finally {
                                emitter.complete();
                            }
                        },
                        () -> {
                            try {
                                // 发送完成事件
                                emitter.send(SseEmitter.event()
                                        .name("complete")
                                        .data("分析完成"));
                            } catch (IOException e) {
                                // 忽略错误
                            } finally {
                                emitter.complete();
                            }
                        }
                );

                // 设置完成和超时处理
                emitter.onCompletion(() -> System.out.println("SSE 连接完成"));
                emitter.onTimeout(() -> {
                    System.out.println("SSE 连接超时");
                    emitter.complete();
                });

            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("初始化失败: " + e.getMessage()));
                } catch (IOException ex) {
                    // 忽略错误
                }
                emitter.complete();
            }
        });

        return emitter;
    }


}
