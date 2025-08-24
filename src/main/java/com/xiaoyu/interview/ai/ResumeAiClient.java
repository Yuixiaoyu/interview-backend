package com.xiaoyu.interview.ai;

import cn.hutool.core.util.StrUtil;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.google.gson.Gson;
import com.xiaoyu.interview.common.ErrorCode;
import com.xiaoyu.interview.config.DashScopeConfig;
import com.xiaoyu.interview.constant.RedisConstant;
import com.xiaoyu.interview.exception.BusinessException;
import com.xiaoyu.interview.model.entity.ResumeDocument;
import com.xiaoyu.interview.utils.RedisUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


/**
 * 简历解析（图片理解模型）
 * pdf--->图片--->图片理解模型--->JSON解析结果
 */
@Component
@Slf4j
public class ResumeAiClient {

    @Resource
    private DashScopeConfig dashScopeConfig;

    @Resource
    private RedisUtil redisUtil;

    /**
     * redis存放简历的过期时间
     */
    private static final Integer RESUME_TIMEOUT = 60 * 10;

    private static final String userPrompt = "我上传了一张求职者简历的图片，请你根据系统提示词中的字段说明，提取图片中的所有信息，并按 JSON 格式返回,不要输出```json```代码段. \n" +
            "要求：\n" +
            "1. JSON 结构严格按照 ResumeDocument 中 resume 的字段输出，不要输出```json```代码段\n" +
            "2. 如果某个信息缺失，使用空字符串 \"\" 或空数组 []。\n" +
            "3. 数组类型即使只有一个元素，也必须保持数组格式。\n" +
            "4. 仅输出 JSON，不要添加任何解释或文字，不要输出```json```代码段\n";

    private static final String systemPrompt = "你是一个专业的简历信息识别模型，你将收到一张求职者简历图片。你的任务是从图片中提取信息并输出符合以下 JSON 结构的数据，字段名严格对应 Java 类 ResumeDocument,不要输出```json```代码段。\n" +
            "\n" +
            "每个字段的含义如下（请从图片中尽可能提取完整信息，如果缺失则使用空字符串 \"\" 或空数组 []）：\n" +
            "\n" +
            "ResumeDocument {\n" +
            "    Resume resume {\n" +
            "\n" +
            "        // 求职目标\n" +
            "        String job_target; \n" +
            "        // 例如：“Java后端开发工程师”，从简历顶部或职业意向处提取\n" +
            "\n" +
            "        // 基本信息\n" +
            "        String basic_info_name;        // 姓名\n" +
            "        String basic_info_email;       // 邮箱地址\n" +
            "        String basic_info_phone;       // 手机号\n" +
            "        String basic_info_github;      // GitHub 链接\n" +
            "        String basic_info_linkedin;    // LinkedIn 链接\n" +
            "        String basic_info_location;    // 所在城市或地区\n" +
            "\n" +
            "        // 教育经历（数组）\n" +
            "        List<Education> education {\n" +
            "            String degree;             // 学位，例如：工学学士\n" +
            "            String major;              // 专业，例如：软件工程\n" +
            "            String university;         // 学校名称\n" +
            "            String period;             // 学习时间区间，例如：“2020.09 - 2024.06”\n" +
            "            String gpa;                // 平均绩点或成绩\n" +
            "            List<String> relevant_courses; // 相关课程名称\n" +
            "        }\n" +
            "\n" +
            "        // 技能（数组）\n" +
            "        List<String> technical_skills_programming_languages; // 编程语言，例如 Java、Python\n" +
            "        List<String> technical_skills_web_development;       // Web开发框架或技术，例如 Spring Boot、React\n" +
            "        List<String> technical_skills_database;             // 数据库技能，例如 MySQL、MongoDB\n" +
            "        List<String> technical_skills_devops;               // DevOps 工具，例如 Docker、Git、Jenkins\n" +
            "        List<String> technical_skills_others;               // 其他技术，例如 Linux、微服务架构、RESTful API\n" +
            "\n" +
            "        // 项目经历（数组）\n" +
            "        List<Project> projects {\n" +
            "            String name;           // 项目名称\n" +
            "            String period;         // 项目周期\n" +
            "            List<String> technologies; // 项目使用的技术或工具\n" +
            "            String description;    // 项目描述\n" +
            "            List<String> achievements; // 项目成果或亮点\n" +
            "        }\n" +
            "\n" +
            "        // 实习经历（数组）\n" +
            "        List<Internship> internships {\n" +
            "            String company;        // 公司名称\n" +
            "            String position;       // 职位名称\n" +
            "            String period;         // 实习时间区间\n" +
            "            List<String> responsibilities; // 实习主要工作内容\n" +
            "        }\n" +
            "\n" +
            "        // 证书（数组）\n" +
            "        List<String> certifications;   // 证书名称，例如：“AWS Certified Cloud Practitioner”\n" +
            "\n" +
            "        // 其他信息（数组）\n" +
            "        List<String> additional_info_languages; // 掌握的语言，例如中文、英语\n" +
            "        List<String> additional_info_interests; // 兴趣爱好，例如算法竞赛、技术博客写作\n" +
            "    }\n" +
            "}\n" +
            "\n" +
            "要求：\n" +
            "1. 数组类型即使只有一个元素，也使用数组格式输出。\n" +
            "2. 严格按照字段名输出 JSON，不要输出```json```代码段。\n" +
            "3. 对于缺失信息使用空字符串 \"\" 或空数组 []。\n" +
            "4. 输出必须为 JSON，仅包含 resume 字段下的数据，不要输出```json```代码段\n" +
            "5. 每个字段都要根据字段含义提取对应内容，例如项目技术字段提取实际使用的技术名。\n";

    @Async
    public void analyzeResume(String localPath, Long userId)
            throws ApiException, NoApiKeyException, UploadFileException, IOException {
        if (StrUtil.isBlank(localPath)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Path path = Paths.get(localPath);
        byte[] imageBytes = Files.readAllBytes(path);
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        MultiModalConversation conv = new MultiModalConversation();
        MultiModalMessage systemMessage = MultiModalMessage.builder().role(Role.SYSTEM.getValue())
                .content(Arrays.asList(Collections.singletonMap("text", systemPrompt))).build();

        MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
                .content(Arrays.asList(
                        new HashMap<String, Object>() {{
                            put("image", "data:image/png;base64," + base64Image);
                        }},
                        new HashMap<String, Object>() {{
                            put("text", userPrompt);
                        }}
                )).build();

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .apiKey(dashScopeConfig.getApiKey())
                .model("qwen-vl-max")
                .messages(Arrays.asList(systemMessage, userMessage))
                .build();

        MultiModalConversationResult result = conv.call(param);
        Map<String, Object> objectMap = result.getOutput().getChoices().get(0).getMessage().getContent().get(0);
        Object text = objectMap.get("text");
        ResumeDocument resumeDocument = new Gson().fromJson(text.toString(), ResumeDocument.class);
        redisUtil.set(RedisConstant.USER_RESUME_REDIS_KEY_PREFIX + userId, resumeDocument, RESUME_TIMEOUT);
    }
}
