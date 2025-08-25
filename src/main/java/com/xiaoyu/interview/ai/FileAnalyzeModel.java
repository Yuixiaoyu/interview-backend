package com.xiaoyu.interview.ai;

import com.xiaoyu.interview.model.entity.ResumeDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

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
public class FileAnalyzeModel {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
            ## 角色：
                你是一名专业的简历信息提取专家。你的任务是仔细分析用户提供的简历文本，准确识别并提取出关键信息，然后将其严格遵循指定的JSON格式进行输出。
            ## 我的目标：
                我将提供一段简历文本，请你帮我将其中的信息提取并填充到下方我提供的JSON结构模板中。
            ## 输出要求与规则：
               1. 严格遵循格式： 你必须使用我提供的** exact JSON 结构**（包括所有字段和嵌套层级）进行输出。不要添加模板中没有的字段，也不要缺少模板中已有的字段。
               2. 内容匹配： 仔细阅读简历文本，将文本信息精准地对应到JSON的各个字段中。
               3. 智能处理：
                 - 数组处理： 对于 education, projects, internships, certifications 等数组字段，如果简历中有多条记录，请全部提取；如果没有找到相关信息，则该字段为空数组 []。
                 - 字段缺失： 如果简历文本中完全没有某个字段的信息（例如，没有提到LinkedIn，或没有实习经历），请将对应字段的值设为空（null 或 空数组 [] 或 空对象 {}，根据字段类型决定，以符合JSON格式为准）。
                 - 内容推断： 对于 job_target（求职目标），它可能不会明确写在简历里，请根据简历的整体内容（如技能、项目经验）进行合理推断。
                 - 缩写处理： 如果简历中使用了缩写（如“北航”），请尽量将其扩展为全称（如“北京航空航天大学”）。
               4. 输出纯净： 最终的输出必须且只能是一个完整的、格式正确的JSON对象，不要包含任何额外的解释、道歉、Markdown代码块标记（如```json）或前言后语。
            ## JSON 模板结构:
            ```json
            {
              "resume": {
                "job_target": "",
                "basic_info": {
                  "name": "",
                  "email": "",
                  "phone": "",
                  "github": "",
                  "linkedin": "",
                  "location": ""
                },
                "education": [
                  {
                    "degree": "",
                    "major": "",
                    "university": "",
                    "period": "",
                    "gpa": "",
                    "relevant_courses": []
                  }
                ],
                "technical_skills": {
                  "programming_languages": [],
                  "web_development": [],
                  "database": [],
                  "devops": [],
                  "others": []
                },
                "projects": [
                  {
                    "name": "",
                    "period": "",
                    "technologies": [],
                    "description": "",
                    "achievements": []
                  }
                ],
                "internships": [
                  {
                    "company": "",
                    "position": "",
                    "period": "",
                    "responsibilities": []
                  }
                ],
                "certifications": [],
                "additional_info": {
                  "languages": [],
                  "interests": []
                }
              }
            }
            ```
            """;
    private final ChatMemory chatMemory;


    /**
     * 初始化chatClient
     *
     * @param ollamaChatModel
     */
    public FileAnalyzeModel(ChatModel ollamaChatModel) {
        //基于内存的memory
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
        chatClient = ChatClient.builder(ollamaChatModel)
                //.defaultSystem(SYSTEM_PROMPT)
                .defaultSystem("你是一名经验丰富的简历专家，主要工作是根据求职者的简历内容提取成json格式的信息，如果对应的json没有信息就不填")
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                        //自定义日志Advisor
                        //new MyLoggerAdvisor( )
                        //自定义推理增量advisor
                        //new ReReadingAdvisor()
                )
                .build();
        this.chatMemory = chatMemory;
    }


    /**
     * AI 分析简历（支持结构化输出）
     *
     * @param message
     * @param chatId
     * @return
     */
    public ResumeDocument analyzeResume(String message, String chatId) {
        ResumeDocument resumeDocument = chatClient.prompt()
                .user(message)
                .system(SYSTEM_PROMPT + "现在，请根据用户输入的简历文本内容进行信息提取和填充：")
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(ResumeDocument.class);
        log.info("resumeDocument:{}", resumeDocument);
        return resumeDocument;
    }

}
