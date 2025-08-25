package com.xiaoyu.interview.service;

import com.xiaoyu.interview.model.entity.AiInterviewRecords;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author fy
 * @description 针对表【ai_interview_records】的数据库操作Service
 * @createDate 2025-08-26 05:53:46
 */
public interface AiInterviewRecordsService extends IService<AiInterviewRecords> {

    /**
     * 获取面试记录
     * @param sessionId
     */
    List<AiInterviewRecords> getInterviewRecordsBySessionId(String sessionId,Long userId);
}
