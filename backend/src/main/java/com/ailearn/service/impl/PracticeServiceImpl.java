package com.ailearn.service.impl;

import com.ailearn.common.AnswerChecker;
import com.ailearn.common.BusinessException;
import com.ailearn.common.Constants;
import com.ailearn.common.PageResult;
import com.ailearn.dto.PracticeStartDTO;
import com.ailearn.dto.PracticeSubmitDTO;
import com.ailearn.dto.QuestionOption;
import com.ailearn.entity.PracticeRecord;
import com.ailearn.entity.PracticeSession;
import com.ailearn.entity.Question;
import com.ailearn.entity.WrongQuestion;
import com.ailearn.mapper.PracticeRecordMapper;
import com.ailearn.mapper.PracticeSessionMapper;
import com.ailearn.mapper.QuestionMapper;
import com.ailearn.mapper.WrongQuestionMapper;
import com.ailearn.service.AiGradingService;
import com.ailearn.service.PracticeService;
import com.ailearn.service.QuestionService;
import com.ailearn.service.WrongBookService;
import com.ailearn.vo.AiGradeResultVO;
import com.ailearn.vo.PracticeSessionVO;
import com.ailearn.vo.PracticeStartVO;
import com.ailearn.vo.PracticeSubmitResultVO;
import com.ailearn.vo.QuestionVO;
import com.ailearn.vo.WrongQuestionVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 智能刷题实现:四种抽题模式 + 即时判分 + 错题本联动。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PracticeServiceImpl implements PracticeService {

    private final PracticeSessionMapper sessionMapper;
    private final PracticeRecordMapper recordMapper;
    private final QuestionMapper questionMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final QuestionService questionService;
    private final AiGradingService aiGradingService;
    private final WrongBookService wrongBookService;
    private final AnswerChecker answerChecker;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PracticeStartVO start(PracticeStartDTO dto, Long userId) {
        String mode = dto.getMode() == null ? Constants.PRACTICE_RANDOM : dto.getMode().toUpperCase();
        int count = dto.getCount() == null ? 10 : Math.max(1, Math.min(dto.getCount(), 50));

        List<Question> questions = switch (mode) {
            case Constants.PRACTICE_KNOWLEDGE -> {
                if (dto.getKnowledgePointId() == null) {
                    throw new BusinessException("按知识点刷题需要指定知识点");
                }
                yield questionMapper.randomQuestions(dto.getSubjectId(), dto.getKnowledgePointId(),
                        null, dto.getType(), count);
            }
            case Constants.PRACTICE_DIFFICULTY -> {
                if (dto.getDifficulty() == null) {
                    throw new BusinessException("按难度刷题需要指定难度");
                }
                yield questionMapper.randomQuestions(dto.getSubjectId(), dto.getKnowledgePointId(),
                        dto.getDifficulty(), dto.getType(), count);
            }
            case Constants.PRACTICE_WRONG -> wrongBookQuestions(userId, dto.getKnowledgePointId(), count);
            default -> questionMapper.randomQuestions(dto.getSubjectId(), dto.getKnowledgePointId(),
                    dto.getDifficulty(), dto.getType(), count);
        };

        if (questions.isEmpty()) {
            throw new BusinessException(Constants.PRACTICE_WRONG.equals(mode)
                    ? "错题本还没有题目,先去做几道题吧" : "没有符合条件的题目,请调整筛选条件");
        }

        PracticeSession session = new PracticeSession();
        session.setUserId(userId);
        session.setSubjectId(dto.getSubjectId());
        session.setKnowledgePointId(dto.getKnowledgePointId());
        session.setMode(mode);
        session.setQuestionCount(questions.size());
        session.setAnsweredCount(0);
        session.setCorrectCount(0);
        session.setTotalScore(BigDecimal.ZERO);
        session.setStatus("ONGOING");
        session.setStartTime(LocalDateTime.now());
        session.setQuestionIds(writeIds(questions));
        sessionMapper.insert(session);

        PracticeStartVO vo = new PracticeStartVO();
        vo.setSessionId(session.getId());
        vo.setMode(mode);
        vo.setTotal(questions.size());
        List<QuestionVO> questionVOs = new ArrayList<>();
        for (Question question : questions) {
            questionVOs.add(toVO(question));
        }
        vo.setQuestions(questionVOs);
        log.info("刷题会话创建:userId={}, sessionId={}, mode={}, 题数={}", userId, session.getId(), mode, questions.size());
        return vo;
    }

    /** 错题本抽题:按最近答错时间排序 */
    private List<Question> wrongBookQuestions(Long userId, Long knowledgePointId, int count) {
        List<WrongQuestion> wrongs = wrongQuestionMapper.selectList(Wrappers.<WrongQuestion>lambdaQuery()
                .eq(WrongQuestion::getUserId, userId)
                .eq(WrongQuestion::getMastered, 0)
                .orderByDesc(WrongQuestion::getLastWrongTime)
                .last("limit " + count));
        if (wrongs.isEmpty()) {
            return List.of();
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (WrongQuestion wrong : wrongs) {
            ids.add(wrong.getQuestionId());
        }
        List<Question> questions = questionMapper.selectBatchIds(ids);
        if (knowledgePointId == null) {
            return questions;
        }
        List<Question> filtered = new ArrayList<>();
        for (Question question : questions) {
            if (knowledgePointId.equals(question.getKnowledgePointId())) {
                filtered.add(question);
            }
        }
        return filtered;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PracticeSubmitResultVO submit(Long sessionId, PracticeSubmitDTO dto, Long userId) {
        PracticeSession session = sessionMapper.selectById(sessionId);
        if (session == null || !userId.equals(session.getUserId())) {
            throw new BusinessException("刷题会话不存在");
        }
        if (!"ONGOING".equals(session.getStatus())) {
            throw new BusinessException("该会话已结束");
        }
        Question question = questionMapper.selectById(dto.getQuestionId());
        if (question == null) {
            throw new BusinessException("题目不存在");
        }

        double fullScore = question.getScore() == null ? 5.0 : question.getScore().doubleValue();
        boolean correct;
        double score;
        String aiFeedback = null;
        List<String> hitPoints = null;
        List<String> missingPoints = null;

        if (Constants.TYPE_SUBJECTIVE.equals(question.getType())) {
            // 主观题:AI 评分,得分达到 60% 视为通过
            AiGradeResultVO grade = aiGradingService.grade(question.getId(), dto.getUserAnswer(), userId);
            score = grade.getScore() == null ? 0 : grade.getScore();
            correct = score >= fullScore * 0.6;
            aiFeedback = grade.getFeedback();
            hitPoints = grade.getHitPoints();
            missingPoints = grade.getMissingPoints();
        } else {
            correct = answerChecker.isCorrect(question.getType(), question.getAnswer(), dto.getUserAnswer());
            score = correct ? fullScore : 0.0;
        }

        PracticeRecord record = new PracticeRecord();
        record.setSessionId(sessionId);
        record.setUserId(userId);
        record.setQuestionId(question.getId());
        record.setUserAnswer(dto.getUserAnswer());
        record.setIsCorrect(correct ? 1 : 0);
        record.setScore(BigDecimal.valueOf(score));
        record.setAiFeedback(aiFeedback);
        record.setTimeSpent(dto.getTimeSpent() == null ? 0 : dto.getTimeSpent());
        recordMapper.insert(record);

        // 题目统计 + 热门排行
        questionService.recordAnswer(question.getId(), correct);

        // 错题本联动(与考试共用同一套逻辑)
        boolean addedToWrongBook = wrongBookService.record(userId, question.getId(), dto.getUserAnswer(), correct);

        // 会话统计
        session.setAnsweredCount((session.getAnsweredCount() == null ? 0 : session.getAnsweredCount()) + 1);
        session.setCorrectCount((session.getCorrectCount() == null ? 0 : session.getCorrectCount()) + (correct ? 1 : 0));
        BigDecimal total = session.getTotalScore() == null ? BigDecimal.ZERO : session.getTotalScore();
        session.setTotalScore(total.add(BigDecimal.valueOf(score)));
        sessionMapper.updateById(session);

        PracticeSubmitResultVO vo = new PracticeSubmitResultVO();
        vo.setQuestionId(question.getId());
        vo.setType(question.getType());
        vo.setCorrect(correct);
        vo.setScore(score);
        vo.setFullScore(fullScore);
        vo.setCorrectAnswer(question.getAnswer());
        vo.setAnalysis(question.getAnalysis());
        vo.setAiFeedback(aiFeedback);
        vo.setHitPoints(hitPoints);
        vo.setMissingPoints(missingPoints);
        vo.setAnsweredCount(session.getAnsweredCount());
        vo.setCorrectCount(session.getCorrectCount());
        vo.setTotalScore(session.getTotalScore().doubleValue());
        vo.setAddedToWrongBook(addedToWrongBook);
        return vo;
    }

    /**
     * 错题本维护已抽到 {@link WrongBookService},与考试模块共用同一套逻辑。
     */
    private boolean updateWrongBook(Long userId, Long questionId, String userAnswer, boolean correct) {
        return wrongBookService.record(userId, questionId, userAnswer, correct);
    }

    @Override
    public void finish(Long sessionId, Long userId) {
        PracticeSession session = sessionMapper.selectById(sessionId);
        if (session == null || !userId.equals(session.getUserId())) {
            throw new BusinessException("刷题会话不存在");
        }
        session.setStatus("FINISHED");
        session.setFinishTime(LocalDateTime.now());
        sessionMapper.updateById(session);
    }

    @Override
    public PageResult<PracticeSessionVO> mySessions(long page, long size, Long userId) {
        Page<PracticeSession> result = sessionMapper.selectPage(Page.of(Math.max(1, page), Math.min(size, 50)),
                Wrappers.<PracticeSession>lambdaQuery()
                        .eq(PracticeSession::getUserId, userId)
                        .orderByDesc(PracticeSession::getId));
        List<PracticeSessionVO> records = new ArrayList<>();
        for (PracticeSession session : result.getRecords()) {
            PracticeSessionVO vo = new PracticeSessionVO();
            copySession(session, vo);
            records.add(vo);
        }
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    public PracticeSessionVO detail(Long sessionId, Long userId) {
        PracticeSession session = sessionMapper.selectById(sessionId);
        if (session == null || !userId.equals(session.getUserId())) {
            throw new BusinessException("刷题会话不存在");
        }
        PracticeSessionVO vo = new PracticeSessionVO();
        copySession(session, vo);

        List<PracticeRecord> records = recordMapper.selectList(Wrappers.<PracticeRecord>lambdaQuery()
                .eq(PracticeRecord::getSessionId, sessionId)
                .orderByAsc(PracticeRecord::getId));
        for (PracticeRecord record : records) {
            Question question = questionMapper.selectById(record.getQuestionId());
            PracticeSessionVO.RecordItem item = new PracticeSessionVO.RecordItem();
            item.setQuestionId(record.getQuestionId());
            item.setUserAnswer(record.getUserAnswer());
            item.setIsCorrect(record.getIsCorrect());
            item.setScore(record.getScore());
            item.setAiFeedback(record.getAiFeedback());
            item.setTimeSpent(record.getTimeSpent());
            if (question != null) {
                item.setStem(question.getStem());
                item.setType(question.getType());
                item.setCorrectAnswer(question.getAnswer());
                item.setAnalysis(question.getAnalysis());
            }
            vo.getRecords().add(item);
        }
        return vo;
    }

    @Override
    public List<WrongQuestionVO> wrongBook(Integer mastered, Long knowledgePointId, int limit, Long userId) {
        return wrongBookService.list(userId, mastered, knowledgePointId, limit);
    }

    @Override
    public void removeFromWrongBook(Long questionId, Long userId) {
        wrongBookService.remove(userId, questionId);
    }

    private void copySession(PracticeSession session, PracticeSessionVO vo) {
        vo.setId(session.getId());
        vo.setSubjectId(session.getSubjectId());
        vo.setKnowledgePointId(session.getKnowledgePointId());
        vo.setMode(session.getMode());
        vo.setQuestionCount(session.getQuestionCount());
        vo.setAnsweredCount(session.getAnsweredCount());
        vo.setCorrectCount(session.getCorrectCount());
        vo.setTotalScore(session.getTotalScore());
        vo.setStatus(session.getStatus());
        vo.setStartTime(session.getStartTime());
        vo.setFinishTime(session.getFinishTime());
    }

    private QuestionVO toVO(Question question) {
        QuestionVO vo = new QuestionVO();
        vo.setId(question.getId());
        vo.setSubjectId(question.getSubjectId());
        vo.setKnowledgePointId(question.getKnowledgePointId());
        vo.setType(question.getType());
        vo.setStem(question.getStem());
        vo.setDifficulty(question.getDifficulty());
        vo.setScore(question.getScore());
        vo.setTags(question.getTags());
        vo.setSource(question.getSource());
        vo.setViewCount(question.getViewCount());
        vo.setCorrectCount(question.getCorrectCount());
        if (question.getOptions() != null && !question.getOptions().isBlank()) {
            try {
                vo.setOptions(objectMapper.readValue(question.getOptions(), new TypeReference<List<QuestionOption>>() {
                }));
            } catch (Exception ex) {
                vo.setOptions(List.of());
            }
        }
        return vo;
    }

    private String writeIds(List<Question> questions) {
        List<Long> ids = new ArrayList<>();
        for (Question question : questions) {
            ids.add(question.getId());
        }
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (Exception ex) {
            return null;
        }
    }
}
