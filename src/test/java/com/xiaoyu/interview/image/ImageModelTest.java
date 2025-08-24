package com.xiaoyu.interview.image;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.aigc.multimodalconversation.OcrOptions;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import org.junit.jupiter.api.Test;
import com.alibaba.dashscope.common.Role;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * ClassName: ImageModelTest
 * Description:
 *
 * @Author: fy
 * @create: 2025-08-23 11:46
 * @version: 1.0
 */
@SpringBootTest
public class ImageModelTest {
    
    @Test
    public void testImageModel(){


        MultiModalConversation conv = new MultiModalConversation();
        Map<String, Object> map = new HashMap<>();
        map.put("image", "https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20241108/ctdzex/biaozhun.jpg");
        // 输入图像的最大像素阈值，超过该值图像会按原比例缩小，直到总像素低于max_pixels
        map.put("max_pixels", "6422528");
        // 输入图像的最小像素阈值，小于该值图像会按原比例放大，直到总像素大于min_pixels
        map.put("min_pixels", "3136");
        // 开启图像自动转正功能
        map.put("enable_rotate", true);
        // 配置内置任务
        OcrOptions ocrOptions = OcrOptions.builder()
                .task(OcrOptions.Task.TEXT_RECOGNITION)
                .build();
        MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
                .content(Arrays.asList(
                        map,
                        // 当ocr_options中的task字段设置为通用文字识别时，模型会以下面text字段中的内容作为Prompt，不支持用户自定义
                        Collections.singletonMap("text", "Please output only the text content from the image without any additional descriptions or formatting."))).build();
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                // 若没有配置环境变量，请用百炼API Key将下行替换为：.apiKey("sk-xxx")
                .apiKey("sk-777bde2164f74fa3a67f39e7e2e57638")
                .model("qwen-vl-ocr-latest")
                .message(userMessage)
                .ocrOptions(ocrOptions)
                .build();
        MultiModalConversationResult result = null;
        try {
            result = conv.call(param);
        } catch (NoApiKeyException | UploadFileException e) {
            throw new RuntimeException(e);
        }
        System.out.println(result.getOutput().getChoices().get(0).getMessage().getContent().get(0).get("text"));
    }

    @Test
    public void testPath(){
        File dir = new File("src/main/resources/resumeImage");
        String uuid = UUID.randomUUID().toString();
        File imageFile = new File(dir, uuid + ".png");

        Path path = Paths.get(imageFile.getAbsolutePath());
        byte[] imageBytes = null;
        try {
            imageBytes = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        System.out.println(base64Image);
    }

    @Test
    public void testConvert(){

    }


}
