package com.xiaoyu.interview.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

import lombok.Data;

/**
 * @TableName ai_interview_records
 */
@TableName(value = "ai_interview_records")
@Data
public class AiInterviewRecords {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 内容
     */
    private String content;

    /**
     * sessionId
     */
    private String sessionId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 类型（问题/答案）
     */
    private String type;

    /**
     * 得分
     */
    private Integer score;

    /**
     * 创建时间
     */
    private Date createTime;
}