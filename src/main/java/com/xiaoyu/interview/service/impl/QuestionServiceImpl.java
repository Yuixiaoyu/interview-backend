package com.xiaoyu.interview.service.impl;

import cn.hutool.core.collection.CollUtil;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOptionsBuilders;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyu.interview.common.ErrorCode;
import com.xiaoyu.interview.constant.CommonConstant;
import com.xiaoyu.interview.constant.RedisConstant;
import com.xiaoyu.interview.exception.BusinessException;
import com.xiaoyu.interview.exception.ThrowUtils;
import com.xiaoyu.interview.mapper.QuestionMapper;
import com.xiaoyu.interview.model.dto.question.QuestionEsDTO;
import com.xiaoyu.interview.model.dto.question.QuestionQueryRequest;
import com.xiaoyu.interview.model.entity.Question;
import com.xiaoyu.interview.model.entity.QuestionBankQuestion;
import com.xiaoyu.interview.model.entity.User;
import com.xiaoyu.interview.model.vo.QuestionVO;
import com.xiaoyu.interview.model.vo.UserVO;
import com.xiaoyu.interview.service.QuestionBankQuestionService;
import com.xiaoyu.interview.service.QuestionService;
import com.xiaoyu.interview.service.UserService;
import com.xiaoyu.interview.utils.RedisUtil;
import com.xiaoyu.interview.utils.SqlUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 题目服务实现
 */
@Service
@Slf4j
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    @Resource
    private UserService userService;

    @Resource
    private QuestionBankQuestionService questionBankQuestionService;

    @Resource
    private ElasticsearchOperations elasticsearchOperations;

    @Resource
    private RedisUtil redisUtil;

    /**
     * 校验数据
     *
     * @param question
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validQuestion(Question question, boolean add) {
        ThrowUtils.throwIf(question == null, ErrorCode.PARAMS_ERROR);
        // todo 从对象中取值
        String title = question.getTitle();
        String content = question.getContent();
        // 创建数据时，参数不能为空
        if (add) {
            // todo 补充校验规则
            ThrowUtils.throwIf(StringUtils.isBlank(title), ErrorCode.PARAMS_ERROR);
        }
        // 修改数据时，有参数则校验
        // todo 补充校验规则
        if (StringUtils.isNotBlank(title)) {
            ThrowUtils.throwIf(title.length() > 80, ErrorCode.PARAMS_ERROR, "标题过长");
        }
        if (StringUtils.isNotBlank(content)) {
            ThrowUtils.throwIf(content.length() > 10240, ErrorCode.PARAMS_ERROR, "内容过长");
        }
    }

    /**
     * 获取查询条件
     *
     * @param questionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        if (questionQueryRequest == null) {
            return queryWrapper;
        }
        // todo 从对象中取值
        Long id = questionQueryRequest.getId();
        Long notId = questionQueryRequest.getNotId();
        String title = questionQueryRequest.getTitle();
        String content = questionQueryRequest.getContent();
        String searchText = questionQueryRequest.getSearchText();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();
        List<String> tagList = questionQueryRequest.getTags();
        Long userId = questionQueryRequest.getUserId();
        String answer = questionQueryRequest.getAnswer();
        // todo 补充需要的查询条件
        // 从多字段中搜索
        if (StringUtils.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("title", searchText).or().like("content", searchText));
        }
        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        queryWrapper.like(StringUtils.isNotBlank(answer), "answer", answer);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tagList)) {
            for (String tag : tagList) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 精确查询
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取题目封装
     *
     * @param question
     * @param request
     * @return
     */
    @Override
    public QuestionVO getQuestionVO(Question question, HttpServletRequest request) {
        // 对象转封装类
        QuestionVO questionVO = QuestionVO.objToVo(question);

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Long userId = question.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        questionVO.setUser(userVO);
        //// 2. 已登录，获取用户点赞、收藏状态
        //long questionId = question.getId();
        //User loginUser = userService.getLoginUserPermitNull(request);
        //if (loginUser != null) {
        //    // 获取点赞
        //    QueryWrapper<QuestionThumb> questionThumbQueryWrapper = new QueryWrapper<>();
        //    questionThumbQueryWrapper.in("questionId", questionId);
        //    questionThumbQueryWrapper.eq("userId", loginUser.getId());
        //    QuestionThumb questionThumb = questionThumbMapper.selectOne(questionThumbQueryWrapper);
        //    questionVO.setHasThumb(questionThumb != null);
        //    // 获取收藏
        //    QueryWrapper<QuestionFavour> questionFavourQueryWrapper = new QueryWrapper<>();
        //    questionFavourQueryWrapper.in("questionId", questionId);
        //    questionFavourQueryWrapper.eq("userId", loginUser.getId());
        //    QuestionFavour questionFavour = questionFavourMapper.selectOne(questionFavourQueryWrapper);
        //    questionVO.setHasFavour(questionFavour != null);
        //}
        // endregion

        return questionVO;
    }

    /**
     * 分页获取题目封装
     *
     * @param questionPage
     * @param request
     * @return
     */
    @Override
    public Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request) {
        List<Question> questionList = questionPage.getRecords();
        Page<QuestionVO> questionVOPage = new Page<>(questionPage.getCurrent(), questionPage.getSize(), questionPage.getTotal());
        if (CollUtil.isEmpty(questionList)) {
            return questionVOPage;
        }
        // 对象列表 => 封装对象列表
        List<QuestionVO> questionVOList = questionList.stream().map(question -> {
            return QuestionVO.objToVo(question);
        }).collect(Collectors.toList());

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = questionList.stream().map(Question::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        //// 2. 已登录，获取用户点赞、收藏状态
        //Map<Long, Boolean> questionIdHasThumbMap = new HashMap<>();
        //Map<Long, Boolean> questionIdHasFavourMap = new HashMap<>();
        //User loginUser = userService.getLoginUserPermitNull(request);
        //if (loginUser != null) {
        //    Set<Long> questionIdSet = questionList.stream().map(Question::getId).collect(Collectors.toSet());
        //    loginUser = userService.getLoginUser(request);
        //    // 获取点赞
        //    QueryWrapper<QuestionThumb> questionThumbQueryWrapper = new QueryWrapper<>();
        //    questionThumbQueryWrapper.in("questionId", questionIdSet);
        //    questionThumbQueryWrapper.eq("userId", loginUser.getId());
        //    List<QuestionThumb> questionQuestionThumbList = questionThumbMapper.selectList(questionThumbQueryWrapper);
        //    questionQuestionThumbList.forEach(questionQuestionThumb -> questionIdHasThumbMap.put(questionQuestionThumb.getQuestionId(), true));
        //    // 获取收藏
        //    QueryWrapper<QuestionFavour> questionFavourQueryWrapper = new QueryWrapper<>();
        //    questionFavourQueryWrapper.in("questionId", questionIdSet);
        //    questionFavourQueryWrapper.eq("userId", loginUser.getId());
        //    List<QuestionFavour> questionFavourList = questionFavourMapper.selectList(questionFavourQueryWrapper);
        //    questionFavourList.forEach(questionFavour -> questionIdHasFavourMap.put(questionFavour.getQuestionId(), true));
        //}
        // 填充信息
        questionVOList.forEach(questionVO -> {
            Long userId = questionVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionVO.setUser(userService.getUserVO(user));
            //questionVO.setHasThumb(questionIdHasThumbMap.getOrDefault(questionVO.getId(), false));
            //questionVO.setHasFavour(questionIdHasFavourMap.getOrDefault(questionVO.getId(), false));
        });
        // endregion

        questionVOPage.setRecords(questionVOList);
        return questionVOPage;
    }

    /**
     * 分页获取题目列表（仅管理员可用）
     *
     * @param questionQueryRequest
     * @return
     */
    @Override
    public Page<Question> listQuestionByPage(QuestionQueryRequest questionQueryRequest) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();

        //题目表的查询条件
        QueryWrapper<Question> queryWrapper = this.getQueryWrapper(questionQueryRequest);

        //根据题库查询题目列表接口
        Long questionBankId = questionQueryRequest.getQuestionBankId();
        if (questionBankId != null) {
            //查询题库题目id
            LambdaQueryWrapper<QuestionBankQuestion> lambdaQueryWrapper = Wrappers.lambdaQuery(QuestionBankQuestion.class)
                    .select(QuestionBankQuestion::getQuestionId)
                    .eq(QuestionBankQuestion::getQuestionBankId, questionBankId);
            List<QuestionBankQuestion> questionList = questionBankQuestionService.list(lambdaQueryWrapper);
            if (CollUtil.isNotEmpty(questionList)) {
                Set<Long> questionIdSet = questionList.stream().map(QuestionBankQuestion::getQuestionId).collect(Collectors.toSet());
                queryWrapper.in("id", questionIdSet);
            } else {
                //题库为空则返回空列表
                return new Page<>(current, size, 0);
            }
        }

        // 查询数据库
        Page<Question> questionPage = this.page(new Page<>(current, size), queryWrapper);
        return questionPage;
    }

    @Override
    public Page<Question> searchFromEs(QuestionQueryRequest questionQueryRequest) {
        // 获取参数
        Long id = questionQueryRequest.getId();
        Long notId = questionQueryRequest.getNotId();
        String searchText = questionQueryRequest.getSearchText();
        List<String> tags = questionQueryRequest.getTags();
        Long questionBankId = questionQueryRequest.getQuestionBankId();
        Long userId = questionQueryRequest.getUserId();
        int current = questionQueryRequest.getCurrent() - 1; // ES 起始页为 0
        int pageSize = questionQueryRequest.getPageSize();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();

        // 1. 构建基础查询 - BoolQuery.Builder 正确使用
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        // 基础过滤 - isDelete=0
        boolQueryBuilder.filter(TermQuery.of(t -> t.field("isDelete").value(0))._toQuery());

        // ID 过滤
        if (id != null) {
            boolQueryBuilder.filter(TermQuery.of(t -> t.field("id").value(id))._toQuery());
        }

        // NOT ID 过滤
        if (notId != null) {
            boolQueryBuilder.mustNot(TermQuery.of(t -> t.field("id").value(notId))._toQuery());
        }

        // UserID 过滤
        if (userId != null) {
            boolQueryBuilder.filter(TermQuery.of(t -> t.field("userId").value(userId))._toQuery());
        }

        // QuestionBankID 过滤
        if (questionBankId != null) {
            boolQueryBuilder.filter(TermQuery.of(t -> t.field("questionBankId").value(questionBankId))._toQuery());
        }

        // 标签过滤
        if (tags != null && !tags.isEmpty()) {
            for (String tag : tags) {
                boolQueryBuilder.filter(TermQuery.of(t -> t.field("tags").value(tag))._toQuery());
            }
        }

        // 关键词搜索
        if (StringUtils.isNotBlank(searchText)) {
            boolQueryBuilder.should(
                    MatchQuery.of(m -> m.field("title").query(searchText))._toQuery()
            );
            boolQueryBuilder.should(
                    MatchQuery.of(m -> m.field("content").query(searchText))._toQuery()
            );
            boolQueryBuilder.should(
                    MatchQuery.of(m -> m.field("answer").query(searchText))._toQuery()
            );
            boolQueryBuilder.minimumShouldMatch("1");
        }

        // 构建排序（ES8 新语法）
        List<SortOptions> sortOptions = new ArrayList<>();
        if (StringUtils.isNotBlank(sortField)) {
            sortOptions.add(SortOptions.of(s -> s.field(f -> f
                    .field(sortField)
                    .order(CommonConstant.SORT_ORDER_ASC.equals(sortOrder) ?
                            SortOrder.Asc : SortOrder.Desc))));
        } else {
            sortOptions.add(SortOptions.of(s -> s.score(sb -> sb
                    .order(SortOrder.Desc)))); // 默认按得分降序
        }

        // 2. 构建排序
        List<SortOptions> sortOptionsList = new ArrayList<>();

        if (StringUtils.isNotBlank(sortField)) {
            SortOrder order = CommonConstant.SORT_ORDER_ASC.equals(sortOrder) ?
                    SortOrder.Asc : SortOrder.Desc;

            sortOptionsList.add(
                    SortOptionsBuilders.field(b -> b.field(sortField).order(order))
            );
        } else {
            // 默认按相关性得分降序
            sortOptionsList.add(
                    SortOptionsBuilders.score(s -> s.order(SortOrder.Desc))
            );
        }

        // 3. 构建查询请求
        NativeQuery query = new NativeQueryBuilder()
                .withQuery(boolQueryBuilder.build()._toQuery())
                .withPageable(PageRequest.of(current, pageSize))
                .withSort(sortOptionsList)
                .build();

        // 4. 执行查询 - 确保使用的是 ElasticsearchOperations
        SearchHits<QuestionEsDTO> searchHits = elasticsearchOperations.search(query, QuestionEsDTO.class);

        // 5. 转换结果集
        List<Question> resourceList = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .filter(Objects::nonNull)
                .map(QuestionEsDTO::dtoToObj)
                .collect(Collectors.toList());

        // 复用 MySQL 的分页对象，封装返回结果
        Page<Question> page = new Page<>();
        page.setTotal(searchHits.getTotalHits());
        if (searchHits.hasSearchHits()) {
            List<SearchHit<QuestionEsDTO>> searchHitList = searchHits.getSearchHits();
            for (SearchHit<QuestionEsDTO> questionEsDTOSearchHit : searchHitList) {
                resourceList.add(QuestionEsDTO.dtoToObj(questionEsDTOSearchHit.getContent()));
            }
        }
        page.setRecords(resourceList);
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchRemoveQuestions(List<Long> questionIdList) {
        ThrowUtils.throwIf(CollUtil.isEmpty(questionIdList),ErrorCode.PARAMS_ERROR);
        //移除题目题库关系
        LambdaQueryWrapper<QuestionBankQuestion> questionBankQuestionLambdaQueryWrapper = Wrappers.lambdaQuery(QuestionBankQuestion.class)
                .in(QuestionBankQuestion::getQuestionId, questionIdList)
                .groupBy(QuestionBankQuestion::getQuestionBankId);
        List<QuestionBankQuestion> questionBankQuestionList = questionBankQuestionService.list(questionBankQuestionLambdaQueryWrapper);
        for (QuestionBankQuestion questionBankQuestion : questionBankQuestionList) {
            questionBankQuestionService.batchRemoveQuestionsFromBank(questionIdList,questionBankQuestion.getQuestionBankId());

        }
        boolean result = removeBatchByIds(questionIdList);
        ThrowUtils.throwIf(!result,ErrorCode.SYSTEM_ERROR,"题目删除失败");

    }

    @Override
    public List<String> getInterviewQuestions(HttpServletRequest request) {
        if (request == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        return redisUtil.getList(RedisConstant.USER_QUESTION_REDIS_KEY_PREFIX + loginUser.getId());
    }

    // 应用过滤条件的辅助方法
    private Consumer<BoolQuery.Builder> applyFilters(
            BoolQuery.Builder builder,
            Long id, Long notId, Long userId,
            Long questionBankId, List<String> tags) {
        return b -> {
            if (id != null) {
                b.must(must -> must.term(t -> t.field("id").value(id)));
            }
            if (notId != null) {
                b.mustNot(mustNot -> mustNot.term(t -> t.field("id").value(notId)));
            }
            if (userId != null) {
                b.must(must -> must.term(t -> t.field("userId").value(userId)));
            }
            if (questionBankId != null) {
                b.must(must -> must.term(t -> t.field("questionBankId").value(questionBankId)));
            }
            if (tags != null && !tags.isEmpty()) {
                for (String tag : tags) {
                    b.must(must -> must.term(t -> t.field("tags").value(tag)));
                }
            }
        };
    }

    // 创建匹配查询的辅助方法
    private Query matchQuery(String field, String text) {
        return MatchQuery.of(m -> m
                .field(field)
                .query(text)
                .analyzer("standard")
        )._toQuery();
    }



}
