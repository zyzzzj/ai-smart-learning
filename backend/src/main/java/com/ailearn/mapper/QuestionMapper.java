package com.ailearn.mapper;

import com.ailearn.entity.Question;
import com.ailearn.vo.HotQuestionVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 题目。动态 SQL 见 resources/mapper/QuestionMapper.xml。
 */
public interface QuestionMapper extends BaseMapper<Question> {

    /**
     * 随机抽取启用题目(随机练习 / 自动组卷)。
     */
    List<Question> randomQuestions(@Param("subjectId") Long subjectId,
                                   @Param("knowledgePointId") Long knowledgePointId,
                                   @Param("difficulty") Integer difficulty,
                                   @Param("type") String type,
                                   @Param("limit") int limit);

    /**
     * 增加作答次数与答对次数(热门题目统计依赖它)。
     */
    @Update("update question set view_count = view_count + 1, correct_count = correct_count + #{correct} where id = #{id}")
    int increaseCounters(@Param("id") Long id, @Param("correct") int correct);

    /**
     * 热门题目:按作答次数倒序。
     */
    List<HotQuestionVO> selectHotQuestions(@Param("subjectId") Long subjectId, @Param("limit") int limit);

    /**
     * 各知识点题量统计。
     */
    List<Map<String, Object>> countGroupByKnowledge(@Param("subjectId") Long subjectId);
}
