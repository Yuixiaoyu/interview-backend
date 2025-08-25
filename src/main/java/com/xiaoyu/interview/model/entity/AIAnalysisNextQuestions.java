package com.xiaoyu.interview.model.entity;

import lombok.Data;

import java.util.List;

@Data
public class AIAnalysisNextQuestions {
    private int score;        // 上一个得分
    private int seq;        // 题号
    private String type;   //类型
    private String question;  // 下一个问题
}
