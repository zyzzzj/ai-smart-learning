package com.ailearn.service;

import com.ailearn.vo.WrongQuestionVO;

import java.util.List;

/**
 * 错题本:刷题与考试共用。
 */
public interface WrongBookService {

    /**
     * 记录一次作答结果。
     *
     * @return true 表示本次被加入错题本(答错)
     */
    boolean record(Long userId, Long questionId, String userAnswer, boolean correct);

    void remove(Long userId, Long questionId);

    List<WrongQuestionVO> list(Long userId, Integer mastered, Long knowledgePointId, int limit);
}
