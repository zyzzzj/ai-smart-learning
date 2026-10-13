package com.ailearn.service;

import com.ailearn.dto.AiGenerateQuestionDTO;
import com.ailearn.vo.GeneratedQuestionVO;

import java.util.List;

/**
 * AI 智能出题。
 */
public interface AiQuestionService {

    /**
     * 按知识点、难度、题型生成题目。
     *
     * @param dto      出题参数
     * @param save     是否入库(来源标记为 AI)
     * @param userId   发起人
     */
    List<GeneratedQuestionVO> generate(AiGenerateQuestionDTO dto, boolean save, Long userId);
}
