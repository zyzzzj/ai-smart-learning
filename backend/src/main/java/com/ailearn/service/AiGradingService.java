package com.ailearn.service;

import com.ailearn.vo.AiGradeResultVO;

/**
 * AI 主观题自动评分。
 */
public interface AiGradingService {

    /**
     * 对某道主观题的作答进行评分。
     *
     * @param questionId    主观题 id
     * @param studentAnswer 学生作答
     * @param userId        学生 id(用于记录任务)
     */
    AiGradeResultVO grade(Long questionId, String studentAnswer, Long userId);
}
