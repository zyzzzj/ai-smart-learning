package com.ailearn.mapper;

import com.ailearn.entity.PracticeRecord;
import com.ailearn.vo.DailyStatVO;
import com.ailearn.vo.KnowledgeStatVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 刷题明细。SQL 见 resources/mapper/PracticeRecordMapper.xml。
 */
public interface PracticeRecordMapper extends BaseMapper<PracticeRecord> {

    /**
     * 按知识点统计正确率(学习画像与 AI 建议输入)。
     */
    List<KnowledgeStatVO> statsByKnowledge(@Param("userId") Long userId);

    /**
     * 每日作答趋势。
     */
    List<DailyStatVO> statsByDay(@Param("userId") Long userId, @Param("begin") LocalDateTime begin);

    /**
     * 累计答题耗时(秒)。
     */
    Long sumTimeSpent(@Param("userId") Long userId);
}
