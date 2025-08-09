package com.xiaoyu.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaoyu.interview.common.BaseResponse;
import com.xiaoyu.interview.model.dto.post.PostQueryRequest;
import com.xiaoyu.interview.model.dto.question.QuestionQueryRequest;
import com.xiaoyu.interview.model.entity.Post;
import com.xiaoyu.interview.model.entity.Question;
import com.xiaoyu.interview.model.vo.QuestionVO;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 题目服务
 *

 */
public interface QuestionService extends IService<Question> {

    /**
     * 校验数据
     *
     * @param question
     * @param add 对创建的数据进行校验
     */
    void validQuestion(Question question, boolean add);

    /**
     * 获取查询条件
     *
     * @param questionQueryRequest
     * @return
     */
    QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest);
    
    /**
     * 获取题目封装
     *
     * @param question
     * @param request
     * @return
     */
    QuestionVO getQuestionVO(Question question, HttpServletRequest request);

    /**
     * 分页获取题目封装
     *
     * @param questionPage
     * @param request
     * @return
     */
    Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request);

    /**
     * 分页获取题目列表（仅管理员可用）
     *
     * @param questionQueryRequest
     * @return
     */
    Page<Question> listQuestionByPage(QuestionQueryRequest questionQueryRequest);



    /**
     * 从 ES 查询
     *
     * @param questionQueryRequest
     * @return
     */
    Page<Question> searchFromEs(QuestionQueryRequest questionQueryRequest);



    /**
     * 批量删除题目
     *
     * @param questionIdList
     */
    void batchRemoveQuestions(List<Long> questionIdList);

}
