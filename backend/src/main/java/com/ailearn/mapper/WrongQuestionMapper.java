package com.ailearn.mapper;

import com.ailearn.entity.WrongQuestion;
import com.ailearn.vo.WrongQuestionVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 错题本。动态 SQL 见 resources/mapper/WrongQuestionMapper.xml。
 */
public interface WrongQuestionMapper extends BaseMapper<WrongQuestion> {

    /**
     * 错题列表(带题目信息)。
     */
    List<WrongQuestionVO> selectWrongList(@Param("userId") Long userId,
                                          @Param("mastered") Integer mastered,
                                          @Param("knowledgePointId") Long knowledgePointId,
                                          @Param("limit") int limit);
}
