package com.ailearn.service.impl;

import com.ailearn.common.CacheNames;
import com.ailearn.common.Constants;
import com.ailearn.entity.ExamRecord;
import com.ailearn.entity.PracticeRecord;
import com.ailearn.entity.WrongQuestion;
import com.ailearn.mapper.ExamRecordMapper;
import com.ailearn.mapper.PracticeRecordMapper;
import com.ailearn.mapper.PracticeSessionMapper;
import com.ailearn.mapper.WrongQuestionMapper;
import com.ailearn.service.LearningStatsService;
import com.ailearn.vo.DailyStatVO;
import com.ailearn.vo.KnowledgeStatVO;
import com.ailearn.vo.LearningStatsVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 学习统计实现:聚合刷题、错题、考试数据,供学习报告与 AI 建议使用。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LearningStatsServiceImpl implements LearningStatsService {

    private final PracticeRecordMapper recordMapper;
    private final PracticeSessionMapper sessionMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final ExamRecordMapper examRecordMapper;

    @Override
    @Cacheable(cacheNames = CacheNames.USER_STATS, key = "#userId")
    public LearningStatsVO stats(Long userId) {
        log.debug("统计学习数据(未命中缓存):userId={}", userId);

        List<PracticeRecord> records = recordMapper.selectList(Wrappers.<PracticeRecord>lambdaQuery()
                .eq(PracticeRecord::getUserId, userId));
        long total = records.size();
        long correct = records.stream().filter(r -> r.getIsCorrect() != null && r.getIsCorrect() == 1).count();

        LearningStatsVO vo = new LearningStatsVO();
        vo.setTotalAnswered(total);
        vo.setTotalCorrect(correct);
        vo.setAccuracy(total == 0 ? 0.0 : Math.round(correct * 10000.0 / total) / 100.0);

        Long sessionCount = sessionMapper.selectCount(Wrappers.<com.ailearn.entity.PracticeSession>lambdaQuery()
                .eq(com.ailearn.entity.PracticeSession::getUserId, userId));
        vo.setSessionCount(sessionCount == null ? 0 : sessionCount);

        Long wrongCount = wrongQuestionMapper.selectCount(Wrappers.<WrongQuestion>lambdaQuery()
                .eq(WrongQuestion::getUserId, userId).eq(WrongQuestion::getMastered, 0));
        Long masteredCount = wrongQuestionMapper.selectCount(Wrappers.<WrongQuestion>lambdaQuery()
                .eq(WrongQuestion::getUserId, userId).eq(WrongQuestion::getMastered, 1));
        vo.setWrongCount(wrongCount == null ? 0 : wrongCount);
        vo.setMasteredCount(masteredCount == null ? 0 : masteredCount);

        List<ExamRecord> examRecords = examRecordMapper.selectList(Wrappers.<ExamRecord>lambdaQuery()
                .eq(ExamRecord::getUserId, userId)
                .in(ExamRecord::getStatus, List.of(Constants.RECORD_SUBMITTED, Constants.RECORD_GRADED)));
        vo.setExamCount((long) examRecords.size());
        if (examRecords.isEmpty()) {
            vo.setExamAvgScore(0.0);
        } else {
            double sum = 0;
            for (ExamRecord record : examRecords) {
                sum += record.getTotalScore() == null ? 0 : record.getTotalScore().doubleValue();
            }
            vo.setExamAvgScore(Math.round(sum / examRecords.size() * 10) / 10.0);
        }

        Long timeSpent = recordMapper.sumTimeSpent(userId);
        vo.setTotalTimeSpent(timeSpent == null ? 0 : timeSpent);

        // 知识点正确率
        List<KnowledgeStatVO> knowledgeStats = recordMapper.statsByKnowledge(userId);
        vo.setKnowledgeStats(knowledgeStats);
        List<String> weak = new ArrayList<>();
        for (KnowledgeStatVO stat : knowledgeStats) {
            if (stat.getRate() != null && stat.getRate() < 70 && stat.getTotal() != null && stat.getTotal() >= 2) {
                weak.add(stat.getKnowledgePointName());
            }
        }
        vo.setWeakPoints(weak);

        // 最近 7 天趋势(补全没有作答的日期)
        LocalDate today = LocalDate.now();
        LocalDateTime begin = LocalDateTime.of(today.minusDays(6), LocalTime.MIN);
        List<DailyStatVO> raw = recordMapper.statsByDay(userId, begin);
        List<DailyStatVO> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            String date = today.minusDays(i).toString();
            DailyStatVO matched = null;
            for (DailyStatVO item : raw) {
                if (date.equals(item.getDate())) {
                    matched = item;
                    break;
                }
            }
            if (matched == null) {
                DailyStatVO empty = new DailyStatVO();
                empty.setDate(date);
                empty.setAnswered(0);
                empty.setCorrect(0);
                trend.add(empty);
            } else {
                trend.add(matched);
            }
        }
        vo.setRecentTrend(trend);
        return vo;
    }
}
