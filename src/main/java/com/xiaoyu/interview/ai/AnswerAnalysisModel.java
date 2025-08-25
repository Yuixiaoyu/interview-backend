package com.xiaoyu.interview.ai;

import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.ResponseFormat;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.xiaoyu.interview.config.DashScopeConfig;
import com.xiaoyu.interview.model.entity.AIAnalysisNextQuestions;
import com.xiaoyu.interview.utils.RedisUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * ClassName: LocalModel
 * Description:
 *    用户回答问题的模型分析，并给出下一题
 * @Author: fy
 * @create: 2025-08-20 22:23
 * @version: 1.0
 */
@Component
@Slf4j
public class AnswerAnalysisModel {

    @Resource
    private DashScopeConfig dashScopeConfig;

    @Resource
    private RedisUtil redisUtil;


    private static final String SYSTEM_PROMPT = """
                    你是一名专业的AI面试官。你会收到以下信息：
                          1. 候选人的简历 JSON。
                          2. 一份基于简历生成的参考题目列表。
                          3. 候选人上一轮的题目。
                          4. 候选人上一轮的回答。
                         ### 你的任务：
                          - 根据参考题目列表和候选人简历，结合候选人的回答，生成下一个最合适的面试问题。
                          - 对候选人的回答进行打分（0~10分），分数越高代表回答越完整、逻辑清晰、技术深度足够。
                          - 你的输出必须是 JSON 对象，且只包含以下两个字段：
                          {
                            "question": "下一个问题文本（不超过50字，简洁明确）",
                            "score": 0-10
                          }
                         ### 评分标准参考
                          9-10: 回答完整、条理清晰、体现深度与实践经验。
                          7-9: 回答合理，有一定深度，但存在不足或缺少细节。
                          5-7: 回答表面化，缺少关键点。
                          0-5: 回答错误、偏题或几乎没有有效内容。
                        ### 规则
                          - 问题要和候选人回答紧密相关，可以选择深入追问，也可以降低难度或切换角度。
                          - 不要一次问多个问题，保持简洁。
                          - 输出必须严格符合 JSON 格式，不要额外解释。
            """;

    /**
     * AI 报告功能（支持结构化输出）
     *
     * @param message
     * @return
     */
    public AIAnalysisNextQuestions generateQuestion(String message,Long userId) throws NoApiKeyException, InputRequiredException {
        Generation gen = new Generation();
        Message systemMsg = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content(SYSTEM_PROMPT)
                .build();
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content(message)
                .build();
        ResponseFormat jsonMode = ResponseFormat.builder().type("json_object").build();
        GenerationParam param = GenerationParam.builder()
                // 若没有配置环境变量，请用阿里云百炼API Key将下行替换为：.apiKey("sk-xxx")
                .apiKey(dashScopeConfig.getApiKey())
                // 此处以qwen-plus为例，可按需更换模型名称。模型列表：https://help.aliyun.com/zh/model-studio/getting-started/models
                .model("qwen-flash")
                .messages(Arrays.asList(systemMsg, userMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .responseFormat(jsonMode)
                .build();
        String jsonStr = gen.call(param).getOutput().getChoices().get(0).getMessage().getContent();
        log.info("jsonStr: {}", jsonStr);
        AIAnalysisNextQuestions result = JSONUtil.toBean(jsonStr, AIAnalysisNextQuestions.class);
        log.info("result: {}", result);
        return result;
    }

}
