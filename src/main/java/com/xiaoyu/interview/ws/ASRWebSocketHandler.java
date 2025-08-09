package com.xiaoyu.interview.ws;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.exception.NoApiKeyException;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class ASRWebSocketHandler extends BinaryWebSocketHandler {

    // 存储每个会话的识别状态
    private final ConcurrentHashMap<String, RecognitionSession> sessions = new ConcurrentHashMap<>();
    
    // 读完标记用于结束流
    private static final ByteBuffer POISON_PILL = ByteBuffer.allocate(0);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 新连接建立时创建识别会话
        sessions.put(session.getId(), new RecognitionSession(session));
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        // 处理接收到的音频数据
        RecognitionSession recognitionSession = sessions.get(session.getId());
        if (recognitionSession != null) {
            recognitionSession.processAudio(message.getPayload());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // 连接关闭时清理资源
        RecognitionSession recognitionSession = sessions.remove(session.getId());
        if (recognitionSession != null) {
            recognitionSession.close();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        // 处理传输错误
        exception.printStackTrace();
        RecognitionSession recognitionSession = sessions.remove(session.getId());
        if (recognitionSession != null) {
            recognitionSession.close();
        }
    }

    // 每个WebSocket连接的识别会话
    private static class RecognitionSession {
        private final WebSocketSession webSocketSession;
        private final BlockingQueue<ByteBuffer> audioQueue = new LinkedBlockingQueue<>();
        private final Flowable<ByteBuffer> audioSource;
        private Disposable recognitionDisposable;

        public RecognitionSession(WebSocketSession session) {
            this.webSocketSession = session;
            
            // 创建音频流Flowable
            this.audioSource = Flowable.create(emitter -> {
                new Thread(() -> {
                    try {
                        while (!Thread.currentThread().isInterrupted()) {
                            ByteBuffer buffer = audioQueue.take();
                            if (buffer == POISON_PILL) {
                                emitter.onComplete();
                                break;
                            }
                            emitter.onNext(buffer);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        emitter.onError(e);
                    }
                }).start();
            }, BackpressureStrategy.BUFFER);
            
            startRecognition();
        }

        public void processAudio(ByteBuffer audioData) {
            // 深拷贝数据避免重用
            ByteBuffer copy = ByteBuffer.allocate(audioData.remaining());
            copy.put(audioData);
            copy.flip();
            audioQueue.add(copy);
        }

        private void startRecognition() {
            try {
                Recognition recognizer = new Recognition();
                RecognitionParam param = RecognitionParam.builder()
                        .model("paraformer-realtime-v2")
                        .apiKey("sk-1f8af108e1ab471e8cd5030da571e566")
                        .parameter("max_sentence_silence", 1500)
                        .format("pcm")
                        .sampleRate(16000)
                        .parameter("language_hints", new String[]{"zh", "en"})
                        .build();

                recognitionDisposable = recognizer.streamCall(param, audioSource)
                        .subscribe(
                                result -> {
                                    try {
                                        String messageType = result.isSentenceEnd() ? "FINAL" : "INTERIM";
                                        String text = result.getSentence().getText();
                                        log.info("识别结果 -: {} - {}", messageType, text);
                                        String jsonResult = String.format(
                                                "{\"type\":\"%s\",\"text\":\"%s\"}", 
                                                messageType, 
                                                escapeJson(text)
                                        );
                                        
                                        if (webSocketSession.isOpen()) {
                                            webSocketSession.sendMessage(new TextMessage(jsonResult));
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                },
                                error -> {
                                    try {
                                        String errorMsg = String.format(
                                                "{\"type\":\"ERROR\",\"message\":\"%s\"}", 
                                                escapeJson(error.getMessage())
                                        );
                                        if (webSocketSession.isOpen()) {
                                            webSocketSession.sendMessage(new TextMessage(errorMsg));
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    close();
                                },
                                () -> {
                                    try {
                                        if (webSocketSession.isOpen()) {
                                            webSocketSession.sendMessage(new TextMessage("{\"type\":\"END\"}"));
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                        );
            } catch (NoApiKeyException e) {
                try {
                    String errorMsg = "{\"type\":\"ERROR\",\"message\":\"Missing API Key\"}";
                    webSocketSession.sendMessage(new TextMessage(errorMsg));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                close();
            }
        }

        public void close() {
            audioQueue.add(POISON_PILL);
            if (recognitionDisposable != null && !recognitionDisposable.isDisposed()) {
                recognitionDisposable.dispose();
            }
        }
        
        private String escapeJson(String input) {
            return input.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t");
        }
    }
}