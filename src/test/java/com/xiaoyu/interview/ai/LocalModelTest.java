package com.xiaoyu.interview.ai;

import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.xiaoyu.interview.model.entity.AIGenerateQuestions;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ClassName: LocalModelTest
 * Description:
 *
 * @Author: fy
 * @create: 2025-06-15 9:59
 * @version: 1.0
 */
@SpringBootTest
class LocalModelTest {

    private static final Logger log = LoggerFactory.getLogger(LocalModelTest.class);
    @Resource
    private LocalModel localModel;
    @Resource
    private ChatModel ollamaChatModel;

    @Resource
    private GenerateQuestionModel generateQuestionModel;


    @Test
    public void testAIResponse() {
        String answer = ollamaChatModel.call(new Prompt("你好呀,我是飞宇"))
                .getResult().getOutput().getText();

        System.out.println(answer);

    }

    @Test
    public void testImage(){
        File dir = new File("src/main/resources/resumeImage");
        String uuid = UUID.randomUUID().toString();
        File imageFile = new File(dir, uuid + ".png");
        String absolutePath = imageFile.getAbsolutePath();
        String correctedPath = absolutePath.replace("\\", "/");
        String filePath = "file:///" + correctedPath;
        System.out.println(filePath);
    }

    @Test
    public void testLocalChatMemory(){
        localModel.startInterview("你好,我是飞宇","1");
        String s = localModel.startInterview("你好，我刚才说我是谁来着？", "1");
        System.out.println(s);

    }

    @Test
    void startInterview() {
        String chatId = UUID.randomUUID().toString();
        //第一轮对话
        String message = "你好面试官，我是飞宇，下面是我的简历内容，请你结合简历内容问我第一个问题：        " +
                "\"基本信息：求职者秋风，男性，党员，本科学历，信息与计算科学专业，2024 年毕业。有个人网站（https://qiufeng.blue）和 GitHub 账号（https://github.com/hua1995116），邮箱为 qiufenghyf@163.com。\\n\" +\n" +
                "                 \"求职意向：前端工程师\\n\" +\n" +
                "                 \"专业技能\\n\" +\n" +
                "                 \"Web 基础：熟练掌握 HTML5/CSS3，了解 ES6/ES7、Webpack，熟悉响应式布局和移动端开发。\\n\" +\n" +
                "                 \"前端框架：有 Vue、React 开发经验，了解其原理与技术栈，有 Antd Design、Element UI、Muse UI 搭建项目经验。\\n\" +\n" +
                "                 \"后端相关：开发过 Node 监控平台中间件，了解常用 Node 模块，能搭建小型 Node 框架，熟悉 TypeScript、ES6/7 进行开发。\\n\" +\n" +
                "                 \"数据库：掌握 MongoDB、Redis。\\n\" +\n" +
                "                 \"项目经验\\n\" +\n" +
                "                 \"前端错误监控系统：作为负责人，完成 web 端 js - sdk 开发、收集端 Node 开发、日志分析及可视化平台搭建，接入量 pv 达 3000w。\\n\" +\n" +
                "                 \"前端性能监控系统：负责人，完成 js - sdk 开发、日志存储及查询聚合模块开发、可视化平台优化，接入 pv 1000w。\\n\" +\n" +
                "                 \"落地页截图项目：利用 puppeteer 和 clustor 多线程开发，提高截图速度，开发自定义队列模式。\\n\" +\n" +
                "                 \"webpack 插件：开发自动将外链改写成内敛形式的插件。\\n\" +\n" +
                "                 \"内网准入系统：采用 TypeScript + ES6/7 + React 开发多层级树形结构的黑名单/白名单控制组件。\\n\" +\n" +
                "                 \"实时聊天项目：负责前端构建、服务器架构和后端开发，聊天室移动端注册用户超 7000 +，GitHub 项目 star 将近 1k。\\n\" +\n" +
                "                 \"工作经历：2018 年 3 月 - 2018 年 7 月在杭州 xx 网络有限公司前端架构组，参与工程化基建搭建，经历 3 次重构，注重性能优化，开发多种组件，拥有日志系统和加密机制，进行请求监控。\\n\" +\n" +
                "                 \"个人优势：xxxx 年开始接触前端，有 Geek 精神和代码洁癖，喜欢前沿技术。获得多项竞赛奖项，主持参与省、国家级项目，发表多篇论文。\\n\" +\n" +
                "    ";
        String answer = localModel.startInterview(message, chatId);
        log.info("AI响应结果：{}",answer);
        ////第二轮对话
        // message = "我想让你帮我分析一下内容中，面试者的面试岗位：【简历信息总结\n" +
        //         "基本信息：求职者秋风，男性，党员，本科学历，信息与计算科学专业，xxxx 年毕业。有个人网站（https://qiufeng.blue）和 GitHub 账号（https://github.com/hua1995116），邮箱为 qiufenghyf@163.com。\n" +
        //         "求职意向：前端工程师\n" +
        //         "专业技能\n" +
        //         "Web 基础：熟练掌握 HTML5/CSS3，了解 ES6/ES7、Webpack，熟悉响应式布局和移动端开发。\n" +
        //         "前端框架：有 Vue、React 开发经验，了解其原理与技术栈，有 Antd Design、Element UI、Muse UI 搭建项目经验。\n" +
        //         "后端相关：开发过 Node 监控平台中间件，了解常用 Node 模块，能搭建小型 Node 框架，熟悉 TypeScript、ES6/7 进行开发。\n" +
        //         "数据库：掌握 MongoDB、Redis。\n" +
        //         "项目经验\n" +
        //         "前端错误监控系统：作为负责人，完成 web 端 js - sdk 开发、收集端 Node 开发、日志分析及可视化平台搭建，接入量 pv 达 3000w。\n" +
        //         "前端性能监控系统：负责人，完成 js - sdk 开发、日志存储及查询聚合模块开发、可视化平台优化，接入 pv 1000w。\n" +
        //         "落地页截图项目：利用 puppeteer 和 clustor 多线程开发，提高截图速度，开发自定义队列模式。\n" +
        //         "webpack 插件：开发自动将外链改写成内敛形式的插件。\n" +
        //         "内网准入系统：采用 TypeScript + ES6/7 + React 开发多层级树形结构的黑名单/白名单控制组件。\n" +
        //         "实时聊天项目：负责前端构建、服务器架构和后端开发，聊天室移动端注册用户超 7000 +，GitHub 项目 star 将近 1k。\n" +
        //         "工作经历：2018 年 3 月 - 2018 年 7 月在杭州 xx 网络有限公司前端架构组，参与工程化基建搭建，经历 3 次重构，注重性能优化，开发多种组件，拥有日志系统和加密机制，进行请求监控。\n" +
        //         "个人优势：xxxx 年开始接触前端，有 Geek 精神和代码洁癖，喜欢前沿技术。获得多项竞赛奖项，主持参与省、国家级项目，发表多篇论文。\n" +
        ////         "需要提升的点\n" +
        //         "简历格式：整体格式稍显混乱，部分内容如内网准入系统描述不完整，影响信息的清晰传达，可进一步优化排版。\n" +
        //         "项目描述：部分项目描述相对简略，如内网准入系统，可补充更多项目背景、目标和成果，突出自己在项目中的具体贡献和解决的问题。\n" +
        //         "技能深度：虽然列出了众多技能，但缺乏对关键技能掌握程度的量化描述，例如可以说明在 Vue、React 等框架中深入掌握的具体特性或实现过的复杂功能。\n" +
        //         "职业规划连贯性：工作经历时间较短，且从 2018 年至今未体现新的工作经历，可能需要补充后续的职业发展情况，以展示职业规划的连贯性。】";
        //answer = localModel.startInterview(message, chatId);
        //第一轮对话
        message = "如果让你来做这个面试官你会问他哪些问题呢";
        answer = localModel.startInterview(message, chatId);

    }

    @Test
    void doInterviewWithReport() {
        String chatId = UUID.randomUUID().toString();
        //第一轮对话
        String message = "我准备好开始面试了";
        String answer = localModel.startInterview(message, chatId);
        System.out.println(answer);
    }

    @Test
    public void testAIGenerate(){
        String userResume = """
                """;
        AIGenerateQuestions aiGenerateQuestions = null;
        try {
            generateQuestionModel.generateQuestion(userResume,1L);
        } catch (NoApiKeyException | InputRequiredException e) {
            throw new RuntimeException(e);
        }
        System.out.println(aiGenerateQuestions);
    }
}