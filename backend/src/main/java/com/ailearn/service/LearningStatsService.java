package com.ailearn.service;

import com.ailearn.vo.LearningStatsVO;

/**
 * 学习统计。
 */
public interface LearningStatsService {

    /** 学生学习统计总览 */
    LearningStatsVO stats(Long userId);
}
