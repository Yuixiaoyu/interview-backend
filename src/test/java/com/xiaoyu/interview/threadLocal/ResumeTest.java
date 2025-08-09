package com.xiaoyu.interview.threadLocal;

import cn.hutool.core.io.FileUtil;
import com.xiaoyu.interview.context.ResumeContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * ClassName: ResumeTest
 * Description:
 *
 * @Author: fy
 * @create: 2025-06-14 21:01
 * @version: 1.0
 */
@SpringBootTest
public class ResumeTest {

    @Test
    public void testResume(){
        ResumeContext.setResumeContextThreadLocal("1231231231231231231adfasd你好");
        ResumeContext.setResumeContextThreadLocal("你好");
        String resumeContextThreadLocal = ResumeContext.getResumeContextThreadLocal();
        System.out.println(resumeContextThreadLocal);
    }


}
