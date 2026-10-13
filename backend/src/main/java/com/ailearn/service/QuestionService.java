package com.ailearn.service;

import com.ailearn.common.PageResult;
import com.ailearn.dto.QuestionDTO;
import com.ailearn.dto.QuestionQueryDTO;
import com.ailearn.entity.Question;
import com.ailearn.vo.QuestionDetailVO;
import com.ailearn.vo.QuestionVO;

import java.util.List;

/**
 * 题库管理。
 */
public interface QuestionService {

    /** 分页查询(管理端,支持多条件筛选) */
    PageResult<QuestionVO> page(QuestionQueryDTO query);

    /** 题目详情(含答案/解析,会累计浏览量并更新热门排行) */
    QuestionDetailVO detail(Long id);

    /** 按 id 列表批量取题目(作答/判分使用,不含答案外泄逻辑) */
    List<Question> listByIds(List<Long> ids);

    Question create(QuestionDTO dto);

    Question update(QuestionDTO dto);

    void remove(Long id);

    void removeBatch(List<Long> ids);

    /** 提交答案后回写统计:作答次数 +1,答对则答对次数 +1 */
    void recordAnswer(Long questionId, boolean correct);
}
