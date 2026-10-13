package com.ailearn.service;

import com.ailearn.vo.HotQuestionVO;

import java.util.List;

/**
 * 热门题目:基于 Redis ZSet 的实时排行。
 */
public interface HotQuestionService {

    /** 记录一次题目访问/作答(写入 ZSet 排行) */
    void record(Long questionId, double score);

    /** 热门题目 TopN(ZSet 为空时回退数据库统计) */
    List<HotQuestionVO> topN(Long subjectId, int limit);
}
