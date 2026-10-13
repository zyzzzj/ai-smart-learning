package com.ailearn.service;

import com.ailearn.vo.AiAdviceVO;

/**
 * AI 学习建议。
 */
public interface AiAdviceService {

    /** 基于学生练习数据生成学习建议并保存 */
    AiAdviceVO generate(Long userId, Long subjectId);

    /** 查询最近一次学习建议 */
    AiAdviceVO latest(Long userId);
}
