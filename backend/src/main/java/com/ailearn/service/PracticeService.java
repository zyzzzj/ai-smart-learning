package com.ailearn.service;

import com.ailearn.common.PageResult;
import com.ailearn.dto.PracticeStartDTO;
import com.ailearn.dto.PracticeSubmitDTO;
import com.ailearn.vo.PracticeSessionVO;
import com.ailearn.vo.PracticeStartVO;
import com.ailearn.vo.PracticeSubmitResultVO;
import com.ailearn.vo.WrongQuestionVO;

import java.util.List;

/**
 * 智能刷题。
 */
public interface PracticeService {

    /** 开始刷题:按模式抽题并创建会话 */
    PracticeStartVO start(PracticeStartDTO dto, Long userId);

    /** 提交单题作答并即时判分(主观题走 AI 评分) */
    PracticeSubmitResultVO submit(Long sessionId, PracticeSubmitDTO dto, Long userId);

    /** 结束会话 */
    void finish(Long sessionId, Long userId);

    /** 我的刷题记录 */
    PageResult<PracticeSessionVO> mySessions(long page, long size, Long userId);

    /** 会话详情(含逐题作答) */
    PracticeSessionVO detail(Long sessionId, Long userId);

    /** 错题本 */
    List<WrongQuestionVO> wrongBook(Integer mastered, Long knowledgePointId, int limit, Long userId);

    /** 从错题本移除(已掌握) */
    void removeFromWrongBook(Long questionId, Long userId);
}
