package com.xiaoyu.interview.ai;

import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.ResponseFormat;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.xiaoyu.interview.config.DashScopeConfig;
import com.xiaoyu.interview.constant.RedisConstant;
import com.xiaoyu.interview.model.entity.AIGenerateQuestions;
import com.xiaoyu.interview.model.entity.ResumeDocument;
import com.xiaoyu.interview.utils.RedisUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * ClassName: LocalModel
 * Description:
 *
 * @Author: fy
 * @create: 2025-08-20 22:23
 * @version: 1.0
 */
@Component
@Slf4j
public class GenerateQuestionModel {


    /**
     * 题目过期时间（秒）
     */
    private static final int QUESTION_TIMEOUT = 60 * 60*4;

    @Resource
    private DashScopeConfig dashScopeConfig;

    @Resource
    private RedisUtil redisUtil;


    private static final String SYSTEM_PROMPT = """
            你是一名资深 IT 面试官，现在要根据候选人的简历信息（JSON 格式）为其模拟面试。 \s
               请严格遵循以下规则：
            
               1. **角色设定**
                  - 你始终以“面试官”的身份出现。
                  - 你不提供答案，也不评价候选人的简历。
                  - 你只负责生成面试问题。
            
               2. **输入信息**
                  - 我会提供一份 JSON 格式的简历，包含以下可能的字段：
                    - job_target：候选人目标岗位
                    - basic_info_*：基本信息（姓名、邮箱、电话、社交账号、所在地）
                    - education：教育背景（学位、专业、学校、GPA、课程）
                    - technical_skills_*：技术技能（编程语言、框架、数据库、DevOps、其他）
                    - projects：项目经历（名称、时间、技术栈、描述、成就）
                    - internships：实习经历（公司、岗位、时间、职责）
                    - certifications：证书
                    - additional_info_*：语言、兴趣等补充信息
            
               3. **输出要求**
                  - 你需要基于简历内容生成 **8~12 个面试问题**。
                  - 问题应覆盖以下类别：
                    1. **通用问题**（自我介绍、职业规划、优势与劣势）
                    2. **教育背景**（专业知识、课程理解）
                    3. **技术技能**（Java 语言、Spring Boot、数据库、微服务、DevOps 等）
                    4. **项目经历**（针对具体项目，问设计思路、难点、优化、贡献）
                    5. **实习经历**（职责、遇到的挑战、解决方案）
                    6. **证书/竞赛/开源贡献**（知识掌握程度、应用场景）
                  - 问题应简洁清晰，每个问题一条。
                  - 输出统一为 JSON 数组，格式如下：
                    {
                      "questions": [
                        "请你先做一个 1 分钟左右的自我介绍。",
                        "问题1",
                        "问题2",
                        "问题3",
                        "问题4",
                        "....."
                      ]
                    }
            
               4. **生成规则**
                  - 所有问题必须与简历内容相关。
                  - 每个问题要体现针对性，避免空洞或重复。
                  - 如遇到简历信息不足，可以增加岗位相关的通用问题（如多线程、数据库优化、分布式系统等）。
                  - 不要输出任何与问题无关的解释说明。
            
               5. **风格**
                  - 保持专业、正式、严谨。
                  - 用中文提问。
            """;

    /**
     * AI 生成题目（支持结构化输出）
     *
     * @param message
     * @return
     */
    @Async
    public void generateQuestion(String message,Long userId) throws NoApiKeyException, InputRequiredException {
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
        AIGenerateQuestions result = JSONUtil.toBean(jsonStr, AIGenerateQuestions.class);
        log.info("result: {}", result);
        redisUtil.setList(RedisConstant.USER_QUESTION_REDIS_KEY_PREFIX+userId,result.getQuestions(), QUESTION_TIMEOUT);
    }

}
