package com.ailearn.mapper;

import com.ailearn.entity.ExamRecord;
import com.ailearn.vo.ScoreRankVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 考试记录。SQL 见 resources/mapper/ExamRecordMapper.xml。
 */
public interface ExamRecordMapper extends BaseMapper<ExamRecord> {

    /**
     * 某场考试的成绩排行。
     */
    List<ScoreRankVO> selectRank(@Param("examId") Long examId, @Param("limit") int limit);

    /**
     * 超时未交卷的记录。
     */
    List<ExamRecord> selectExpired();
}
