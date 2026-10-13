package com.ailearn.mapper;

import com.ailearn.entity.ExamQuestion;
import com.ailearn.vo.ExamQuestionVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 试卷题目。
 */
public interface ExamQuestionMapper extends BaseMapper<ExamQuestion> {

    /**
     * 试卷题目列表(含本场分值,不含答案)。
     */
    List<ExamQuestionVO> selectPaperQuestions(@Param("examId") Long examId);
}
