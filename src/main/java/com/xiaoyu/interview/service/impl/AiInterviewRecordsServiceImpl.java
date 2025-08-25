package com.xiaoyu.interview.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyu.interview.model.entity.AiInterviewRecords;
import com.xiaoyu.interview.service.AiInterviewRecordsService;
import com.xiaoyu.interview.mapper.AiInterviewRecordsMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author fy
 * @description 针对表【ai_interview_records】的数据库操作Service实现
 * @createDate 2025-08-26 05:53:46
 */
@Service
public class AiInterviewRecordsServiceImpl extends ServiceImpl<AiInterviewRecordsMapper, AiInterviewRecords>
        implements AiInterviewRecordsService {

    @Override
    public List<AiInterviewRecords> getInterviewRecordsBySessionId(String sessionId,Long userId) {

        LambdaQueryWrapper<AiInterviewRecords> queryWrapper = Wrappers.lambdaQuery(AiInterviewRecords.class).eq(AiInterviewRecords::getUserId, userId)
                .eq(AiInterviewRecords::getSessionId, sessionId);
        return this.list(queryWrapper);
    }
}




