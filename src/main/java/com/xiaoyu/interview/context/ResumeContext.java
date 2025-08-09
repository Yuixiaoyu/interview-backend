package com.xiaoyu.interview.context;

import lombok.Data;

/**
 * ClassName: ResumeContext
 * Description:
 *
 * @Author: fy
 * @create: 2025-06-14 20:58
 * @version: 1.0
 */

public class ResumeContext {

    private static ThreadLocal<String> resumeContextThreadLocal = new ThreadLocal<>();

    private ResumeContext(){}

    public static void clear() {
        resumeContextThreadLocal.remove();
    }

    public static void setResumeContextThreadLocal(String resumeContext) {
        resumeContextThreadLocal.set(resumeContext);
    }

    public static String getResumeContextThreadLocal() {
       return resumeContextThreadLocal.get();
    }


}
