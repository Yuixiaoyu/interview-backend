package com.xiaoyu.interview.model.dto.question;

import com.xiaoyu.interview.model.entity.Question;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量删除题目请求
 */
@Data
public class QuestionBatchRemoveRequest implements Serializable {

    /**
     * 题目id列表
     */
    private List<Long> questionList;

    private static final long serialVersionUID = 1L;
}