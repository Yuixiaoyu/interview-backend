package com.xiaoyu.interview.model.dto.questionbankquestion;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量向题库创建题目请求
 *
 */
@Data
public class QuestionBankQuestionBatchAddRequest implements Serializable {

    /**
     * 题库 id
     */
    private Long questionBankId;

    /**
     * 题目 id列表
     */
    private List<Long> questionIdList;

    private static final long serialVersionUID = 1L;
}