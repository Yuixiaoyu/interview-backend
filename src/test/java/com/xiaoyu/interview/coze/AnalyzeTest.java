package com.xiaoyu.interview.coze;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xiaoyu.interview.config.StreamDataCallback;
import com.xiaoyu.interview.service.impl.CozeService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * ClassName: AnalyzeTest
 * Description:
 *
 * @Author: fy
 * @create: 2025-06-12 19:27
 * @version: 1.0
 */
@SpringBootTest
public class AnalyzeTest {


    @Resource
    private CozeService cozeService;

    @Test
    public void testAnalyze(){
        // 发起请求
        cozeService.analyzeFileStream("https://interview-1317444877.cos.ap-chengdu.myqcloud.com/interview/5/cRIbtJYw-我的简历.pdf", new StreamDataCallback() {
            @Override
            public void onData(String data) {
                // 实时处理每个data块（如解析JSON并输出）
                JSONObject jsonData = JSONUtil.parseObj(data);
                String content = jsonData.getStr("content");
                System.out.println("实时内容：" + content);
            }

            @Override
            public void onComplete() {
                System.out.println("流式输出结束");
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("处理失败"+throwable.getMessage());
            }
        });
    }

}
