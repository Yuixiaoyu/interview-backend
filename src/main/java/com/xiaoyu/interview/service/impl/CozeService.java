package com.xiaoyu.interview.service.impl;

import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import com.xiaoyu.interview.common.ErrorCode;
import com.xiaoyu.interview.config.CozeConfig;
import com.xiaoyu.interview.config.StreamDataCallback;
import com.xiaoyu.interview.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

// 3. 服务类
@Service
@RequiredArgsConstructor
public class CozeService {

    private final CozeConfig cozeConfig;

    /**
     * 发送流式请求，通过回调处理实时数据
     * @param fileUrl 文件URL
     * @param callback 数据回调接口
     */
    public void analyzeFileStream(String fileUrl, StreamDataCallback callback) {
        JSONObject requestData = generateRequestData(fileUrl);

        try {
            // 发送POST请求并获取响应
            HttpResponse response = HttpRequest.post("https://api.coze.cn/v1/workflow/stream_run")
                    .header("Authorization", "Bearer " + cozeConfig.getToken())
                    .header("Content-Type", "application/json")
                    .body(requestData.toString())
                    .executeAsync();

            // 检查HTTP状态码（非200视为请求失败）
            if (response.getStatus() != HttpStatus.OK.value()) {
                String errorMsg = "流式请求失败，状态码：" + response.getStatus() + "，响应：" + response.body();
                callback.onError(new BusinessException(ErrorCode.SYSTEM_ERROR, errorMsg));
                return;
            }

            // 读取响应流并逐行处理
            try (InputStream inputStream = response.bodyStream();
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue; // 跳过空行

                    // 解析data字段（关键数据）
                    if (line.startsWith("data: ")) {
                        String dataContent = line.substring("data: ".length()).trim();
                        if (!dataContent.isEmpty()) {
                            callback.onData(dataContent); // 回调实时数据
                        }
                    }
                    // 可选：解析event字段（如检测流结束）
                    else if (line.startsWith("event: ")) {
                        String eventType = line.substring("event: ".length()).trim();
                        if ("Done".equals(eventType)) {
                            callback.onComplete(); // 流结束回调
                            break; // 退出循环，停止读取
                        }
                    }
                }
            } catch (IOException e) {
                callback.onError(new BusinessException(ErrorCode.SYSTEM_ERROR, "读取响应流失败"+e.getMessage()));
            }

        } catch (HttpException e) {
            callback.onError(new BusinessException(ErrorCode.SYSTEM_ERROR, "HTTP请求异常"+e.getMessage()));
        }
    }

    /**
     * 生成请求体JSON（原逻辑复用）
     */
    private JSONObject generateRequestData(String fileUrl) {
        JSONObject parameters = new JSONObject();
        parameters.set("fileUrl", fileUrl);

        JSONObject requestData = new JSONObject();
        requestData.set("workflow_id", cozeConfig.getClientId());
        requestData.set("parameters", parameters);
        return requestData;
    }
}