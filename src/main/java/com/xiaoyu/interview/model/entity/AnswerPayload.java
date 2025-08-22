package com.xiaoyu.interview.model.entity;

import lombok.Data;

@Data
public class AnswerPayload {
    private int seq;        // 题号
    private String answer;  // 用户回答的字符串
}