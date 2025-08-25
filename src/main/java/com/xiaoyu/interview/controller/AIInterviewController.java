package com.xiaoyu.interview.controller;


import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaoyu.interview.common.BaseResponse;
import com.xiaoyu.interview.common.ErrorCode;
import com.xiaoyu.interview.common.ResultUtils;
import com.xiaoyu.interview.config.StreamDataCallback;
import com.xiaoyu.interview.context.ResumeContext;
import com.xiaoyu.interview.exception.BusinessException;
import com.xiaoyu.interview.model.entity.AiInterviewRecords;
import com.xiaoyu.interview.model.entity.AiSession;
import com.xiaoyu.interview.service.AiInterviewRecordsService;
import com.xiaoyu.interview.service.AiSessionService;
import com.xiaoyu.interview.service.impl.CozeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ClassName: AIInterviewController
 * Description:
 *
 * @Author: fy
 * @create: 2025-06-12 20:17
 * @version: 1.0
 */
@RestController
@Slf4j
@RequestMapping("/interview")
public class AIInterviewController {

    @Resource
    private AiSessionService aiSessionService;

    @Resource
    private AiInterviewRecordsService aiInterviewRecordsService;

    /**
     * 获取会话记录
     * @return
     */
    @GetMapping(value = "/get/session")
    public BaseResponse<List<AiSession>> getInterviewSession() {
        long userId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<AiSession> queryWrapper = Wrappers.lambdaQuery(AiSession.class).eq(AiSession::getUserId, userId);
        List<AiSession> aiSessionList = aiSessionService.list(queryWrapper);
        return ResultUtils.success(aiSessionList);
    }

    /**
     * 根据会话Id获取面试记录
     * @return
     */
    @GetMapping(value = "/get/detail")
    public BaseResponse<List<AiInterviewRecords>> getInterviewDetailBySessionId(@RequestParam("sessionId") String sessionId) {

        if (StrUtil.isBlank(sessionId)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long userId = StpUtil.getLoginIdAsLong();
        return ResultUtils.success(aiInterviewRecordsService.getInterviewRecordsBySessionId(sessionId,userId));
    }


}
