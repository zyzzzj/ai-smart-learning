package com.ailearn.service.impl;

import com.ailearn.common.AnswerChecker;
import com.ailearn.common.BusinessException;
import com.ailearn.common.CacheNames;
import com.ailearn.common.Constants;
import com.ailearn.common.PageResult;
import com.ailearn.dto.ExamAutoComposeDTO;
import com.ailearn.dto.ExamDTO;
import com.ailearn.dto.ExamSubmitDTO;
import com.ailearn.dto.QuestionOption;
import com.ailearn.entity.Exam;
import com.ailearn.entity.ExamAnswer;
import com.ailearn.entity.ExamQuestion;
import com.ailearn.entity.ExamRecord;
import com.ailearn.entity.Question;
import com.ailearn.entity.Subject;
import com.ailearn.mapper.ExamAnswerMapper;
import com.ailearn.mapper.ExamMapper;
import com.ailearn.mapper.ExamQuestionMapper;
import com.ailearn.mapper.ExamRecordMapper;
import com.ailearn.mapper.QuestionMapper;
import com.ailearn.mapper.SubjectMapper;
import com.ailearn.service.AiAdviceService;
import com.ailearn.service.AiGradingService;
import com.ailearn.service.ExamService;
import com.ailearn.service.QuestionService;
import com.ailearn.service.WrongBookService;
import com.ailearn.vo.AiGradeResultVO;
import com.ailearn.vo.ExamPaperVO;
import com.ailearn.vo.ExamQuestionVO;
import com.ailearn.vo.ExamResultVO;
import com.ailearn.vo.ExamVO;
import com.ailearn.vo.ScoreRankVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 智能考试实现:手动/AI 组卷、限时考试、客观题自动判分、主观题 AI 评分、AI 学习建议、成绩排行。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExamServiceImpl implements ExamService {

    private final ExamMapper examMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamAnswerMapper examAnswerMapper;
    private final QuestionMapper questionMapper;
    private final SubjectMapper subjectMapper;
    private final AnswerChecker answerChecker;
    private final AiGradingService aiGradingService;
    private final AiAdviceService aiAdviceService;
    private final QuestionService questionService;
    private final WrongBookService wrongBookService;
    private final ObjectMapper objectMapper;

    /* ---------------- 管理端 ---------------- */

    @Override
    public PageResult<ExamVO> page(String keyword, String status, long page, long size, Long userId, boolean admin) {
        Page<Exam> result = examMapper.selectPage(Page.of(Math.max(1, page), Math.min(size, 50)),
                Wrappers.<Exam>lambdaQuery()
                        .like(keyword != null && !keyword.isBlank(), Exam::getName, keyword)
                        .eq(status != null && !status.isBlank(), Exam::getStatus, status)
                        .eq(!admin, Exam::getStatus, Constants.EXAM_PUBLISHED)
                        .orderByDesc(Exam::getId));

        Map<Long, String> subjectNames = new LinkedHashMap<>();
        List<ExamVO> records = new ArrayList<>();
        for (Exam exam : result.getRecords()) {
            ExamVO vo = toVO(exam, subjectNames);
            if (!admin && userId != null) {
                ExamRecord record = examRecordMapper.selectOne(Wrappers.<ExamRecord>lambdaQuery()
                        .eq(ExamRecord::getExamId, exam.getId())
                        .eq(ExamRecord::getUserId, userId));
                if (record == null) {
                    vo.setMyRecordStatus("未参加");
                } else {
                    vo.setMyRecordStatus(record.getStatus());
                    vo.setMyScore(record.getTotalScore());
                }
            }
            records.add(vo);
        }
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    private ExamVO toVO(Exam exam, Map<Long, String> subjectNames) {
        ExamVO vo = new ExamVO();
        vo.setId(exam.getId());
        vo.setName(exam.getName());
        vo.setSubjectId(exam.getSubjectId());
        vo.setDescription(exam.getDescription());
        vo.setDuration(exam.getDuration());
        vo.setTotalScore(exam.getTotalScore());
        vo.setPassScore(exam.getPassScore());
        vo.setQuestionCount(exam.getQuestionCount());
        vo.setStartTime(exam.getStartTime());
        vo.setEndTime(exam.getEndTime());
        vo.setStatus(exam.getStatus());
        vo.setCreateTime(exam.getCreateTime());
        if (exam.getSubjectId() != null) {
            String name = subjectNames.computeIfAbsent(exam.getSubjectId(), id -> {
                Subject subject = subjectMapper.selectById(id);
                return subject == null ? null : subject.getName();
            });
            vo.setSubjectName(name);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = CacheNames.EXAM_DETAIL, allEntries = true)
    public Exam create(ExamDTO dto, Long userId) {
        if (dto.getQuestions() == null || dto.getQuestions().isEmpty()) {
            throw new BusinessException("请至少选择一道题目");
        }
        Subject subject = subjectMapper.selectById(dto.getSubjectId());
        if (subject == null) {
            throw new BusinessException("科目不存在");
        }
        Exam exam = new Exam();
        exam.setName(dto.getName());
        exam.setSubjectId(dto.getSubjectId());
        exam.setDescription(dto.getDescription());
        exam.setDuration(dto.getDuration() == null ? 60 : dto.getDuration());
        exam.setQuestionCount(dto.getQuestions().size());
        exam.setStatus(Constants.EXAM_DRAFT);
        exam.setCreateUser(userId);
        exam.setStartTime(dto.getStartTime());
        exam.setEndTime(dto.getEndTime());
        exam.setTotalScore(BigDecimal.ZERO);
        examMapper.insert(exam);

        BigDecimal total = saveExamQuestions(exam.getId(), dto.getQuestions());
        exam.setTotalScore(total);
        exam.setPassScore(dto.getPassScore() == null ? defaultPassScore(total) : dto.getPassScore());
        examMapper.updateById(exam);
        return exam;
    }

    private BigDecimal saveExamQuestions(Long examId, List<ExamDTO.ExamQuestionItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        int sort = 1;
        Set<Long> used = new LinkedHashSet<>();
        for (ExamDTO.ExamQuestionItem item : items) {
            if (item.getQuestionId() == null || !used.add(item.getQuestionId())) {
                continue;
            }
            Question question = questionMapper.selectById(item.getQuestionId());
            if (question == null) {
                continue;
            }
            BigDecimal score = item.getScore() != null ? item.getScore()
                    : (question.getScore() == null ? new BigDecimal("5") : question.getScore());
            ExamQuestion examQuestion = new ExamQuestion();
            examQuestion.setExamId(examId);
            examQuestion.setQuestionId(item.getQuestionId());
            examQuestion.setScore(score);
            examQuestion.setSort(item.getSort() == null ? sort++ : item.getSort());
            examQuestionMapper.insert(examQuestion);
            total = total.add(score);
        }
        return total;
    }

    private BigDecimal defaultPassScore(BigDecimal total) {
        return total.multiply(new BigDecimal("0.6")).setScale(1, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = CacheNames.EXAM_DETAIL, allEntries = true)
    public Exam autoCompose(ExamAutoComposeDTO dto, Long userId) {
        int target = dto.getQuestionCount() == null ? 10 : Math.max(1, Math.min(dto.getQuestionCount(), 50));
        List<Question> picked = new ArrayList<>();
        Set<Long> pickedIds = new LinkedHashSet<>();

        List<Long> knowledgeIds = dto.getKnowledgePointIds();
        Long knowledgeId = (knowledgeIds == null || knowledgeIds.isEmpty()) ? null : knowledgeIds.get(0);

        // 1. 按题型配比抽题
        if (dto.getTypeDistribution() != null && !dto.getTypeDistribution().isEmpty()) {
            for (Map.Entry<String, Integer> entry : dto.getTypeDistribution().entrySet()) {
                int want = entry.getValue() == null ? 0 : entry.getValue();
                if (want <= 0) {
                    continue;
                }
                List<Question> found = questionMapper.randomQuestions(dto.getSubjectId(), knowledgeId, null, entry.getKey(), want);
                for (Question question : found) {
                    if (pickedIds.add(question.getId())) {
                        picked.add(question);
                    }
                }
            }
        }
        // 2. 按难度配比抽题
        if (dto.getDifficultyDistribution() != null && !dto.getDifficultyDistribution().isEmpty()) {
            for (Map.Entry<Integer, Integer> entry : dto.getDifficultyDistribution().entrySet()) {
                int want = entry.getValue() == null ? 0 : entry.getValue();
                if (want <= 0) {
                    continue;
                }
                List<Question> found = questionMapper.randomQuestions(dto.getSubjectId(), knowledgeId, entry.getKey(), null, want);
                for (Question question : found) {
                    if (pickedIds.add(question.getId())) {
                        picked.add(question);
                    }
                }
            }
        }
        // 3. 不足时用随机题补齐
        if (picked.size() < target) {
            List<Question> fill = questionMapper.randomQuestions(dto.getSubjectId(), knowledgeId, null, null, target * 2);
            for (Question question : fill) {
                if (picked.size() >= target) {
                    break;
                }
                if (pickedIds.add(question.getId())) {
                    picked.add(question);
                }
            }
        }
        if (picked.isEmpty()) {
            throw new BusinessException("题库中没有符合条件的题目,请先补充题目或用 AI 出题");
        }
        if (picked.size() > target) {
            picked = new ArrayList<>(picked.subList(0, target));
        }

        // 主观题分值给高一些,保证总分合理
        List<ExamDTO.ExamQuestionItem> items = new ArrayList<>();
        int sort = 1;
        for (Question question : picked) {
            ExamDTO.ExamQuestionItem item = new ExamDTO.ExamQuestionItem();
            item.setQuestionId(question.getId());
            item.setScore(Constants.TYPE_SUBJECTIVE.equals(question.getType())
                    ? new BigDecimal("10") : question.getScore());
            item.setSort(sort++);
            items.add(item);
        }

        ExamDTO dto2 = new ExamDTO();
        dto2.setName(dto.getName());
        dto2.setSubjectId(dto.getSubjectId());
        dto2.setDescription(dto.getDescription() == null ? "AI 自动组卷生成" : dto.getDescription());
        dto2.setDuration(dto.getDuration());
        dto2.setPassScore(dto.getPassScore());
        dto2.setQuestions(items);
        Exam exam = create(dto2, userId);
        log.info("AI 组卷完成:examId={}, 题数={}, 总分={}", exam.getId(), items.size(), exam.getTotalScore());
        return exam;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = CacheNames.EXAM_DETAIL, allEntries = true)
    public Exam update(ExamDTO dto) {
        Exam exam = examMapper.selectById(dto.getId());
        if (exam == null) {
            throw new BusinessException("考试不存在");
        }
        if (!Constants.EXAM_DRAFT.equals(exam.getStatus())) {
            throw new BusinessException("只有草稿状态的考试可以修改");
        }
        exam.setName(dto.getName());
        exam.setSubjectId(dto.getSubjectId());
        exam.setDescription(dto.getDescription());
        exam.setDuration(dto.getDuration());
        exam.setStartTime(dto.getStartTime());
        exam.setEndTime(dto.getEndTime());
        examMapper.updateById(exam);

        if (dto.getQuestions() != null && !dto.getQuestions().isEmpty()) {
            examQuestionMapper.delete(Wrappers.<ExamQuestion>lambdaQuery().eq(ExamQuestion::getExamId, exam.getId()));
            BigDecimal total = saveExamQuestions(exam.getId(), dto.getQuestions());
            exam.setTotalScore(total);
            exam.setQuestionCount(dto.getQuestions().size());
            exam.setPassScore(dto.getPassScore() == null ? defaultPassScore(total) : dto.getPassScore());
            examMapper.updateById(exam);
        }
        return examMapper.selectById(exam.getId());
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.EXAM_DETAIL, allEntries = true)
    public void changeStatus(Long examId, String status) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new BusinessException("考试不存在");
        }
        if (!List.of(Constants.EXAM_DRAFT, Constants.EXAM_PUBLISHED, Constants.EXAM_CLOSED).contains(status)) {
            throw new BusinessException("不支持的状态:" + status);
        }
        exam.setStatus(status);
        examMapper.updateById(exam);
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.EXAM_DETAIL, allEntries = true)
    public void remove(Long examId) {
        examMapper.deleteById(examId);
        examQuestionMapper.delete(Wrappers.<ExamQuestion>lambdaQuery().eq(ExamQuestion::getExamId, examId));
    }

    @Override
    public List<ScoreRankVO> rank(Long examId, int limit) {
        return examRecordMapper.selectRank(examId, Math.max(1, Math.min(limit, 100)));
    }

    /* ---------------- 学生端 ---------------- */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamPaperVO start(Long examId, Long userId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new BusinessException("考试不存在");
        }
        if (!Constants.EXAM_PUBLISHED.equals(exam.getStatus())) {
            throw new BusinessException("该考试尚未发布或已关闭");
        }
        LocalDateTime now = LocalDateTime.now();
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            throw new BusinessException("考试尚未开始,开始时间:" + exam.getStartTime());
        }
        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            throw new BusinessException("考试已截止");
        }

        ExamRecord record = examRecordMapper.selectOne(Wrappers.<ExamRecord>lambdaQuery()
                .eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getUserId, userId));
        if (record != null && !Constants.RECORD_ONGOING.equals(record.getStatus())) {
            throw new BusinessException("你已完成本场考试,请到考试记录中查看成绩");
        }
        if (record == null) {
            record = new ExamRecord();
            record.setExamId(examId);
            record.setUserId(userId);
            record.setStartTime(now);
            record.setObjectiveScore(BigDecimal.ZERO);
            record.setSubjectiveScore(BigDecimal.ZERO);
            record.setTotalScore(BigDecimal.ZERO);
            record.setCorrectCount(0);
            record.setStatus(Constants.RECORD_ONGOING);
            examRecordMapper.insert(record);
        }

        ExamPaperVO paper = new ExamPaperVO();
        paper.setExamId(exam.getId());
        paper.setName(exam.getName());
        paper.setDescription(exam.getDescription());
        paper.setSubjectId(exam.getSubjectId());
        paper.setDuration(exam.getDuration());
        paper.setTotalScore(exam.getTotalScore());
        paper.setPassScore(exam.getPassScore());
        paper.setRecordId(record.getId());
        paper.setStartTime(record.getStartTime());

        long elapsed = Duration.between(record.getStartTime(), now).getSeconds();
        long allowed = (exam.getDuration() == null ? 60 : exam.getDuration()) * 60L;
        paper.setRemainingSeconds(Math.max(0, allowed - elapsed));

        // 已暂存的作答
        List<ExamAnswer> saved = examAnswerMapper.selectList(Wrappers.<ExamAnswer>lambdaQuery()
                .eq(ExamAnswer::getExamRecordId, record.getId()));
        Map<Long, String> savedMap = new LinkedHashMap<>();
        for (ExamAnswer answer : saved) {
            if (answer.getUserAnswer() != null) {
                savedMap.put(answer.getQuestionId(), answer.getUserAnswer());
            }
        }
        paper.setSavedCount(savedMap.size());

        List<ExamQuestionVO> questions = examQuestionMapper.selectPaperQuestions(examId);
        for (ExamQuestionVO question : questions) {
            question.setOptions(parseOptions(question));
            question.setUserAnswer(savedMap.get(question.getQuestionId()));
        }
        paper.setQuestions(questions);
        return paper;
    }

    private List<QuestionOption> parseOptions(ExamQuestionVO vo) {
        String json = vo.getOptions() == null ? null : String.valueOf(vo.getOptions());
        if (json == null || json.isBlank() || "null".equals(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<QuestionOption>>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAnswer(Long examId, Long questionId, String userAnswer, Long userId) {
        ExamRecord record = requireOngoingRecord(examId, userId);
        upsertAnswer(record.getId(), questionId, userAnswer);
    }

    private ExamRecord requireOngoingRecord(Long examId, Long userId) {
        ExamRecord record = examRecordMapper.selectOne(Wrappers.<ExamRecord>lambdaQuery()
                .eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getUserId, userId));
        if (record == null) {
            throw new BusinessException("请先开始考试");
        }
        if (!Constants.RECORD_ONGOING.equals(record.getStatus())) {
            throw new BusinessException("本场考试已结束");
        }
        return record;
    }

    private void upsertAnswer(Long recordId, Long questionId, String userAnswer) {
        ExamAnswer exists = examAnswerMapper.selectOne(Wrappers.<ExamAnswer>lambdaQuery()
                .eq(ExamAnswer::getExamRecordId, recordId)
                .eq(ExamAnswer::getQuestionId, questionId));
        if (exists == null) {
            ExamAnswer answer = new ExamAnswer();
            answer.setExamRecordId(recordId);
            answer.setQuestionId(questionId);
            answer.setUserAnswer(userAnswer);
            answer.setScore(BigDecimal.ZERO);
            answer.setGraded(0);
            examAnswerMapper.insert(answer);
        } else {
            exists.setUserAnswer(userAnswer);
            examAnswerMapper.updateById(exists);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamResultVO submit(Long examId, ExamSubmitDTO dto, Long userId) {
        ExamRecord record = requireOngoingRecord(examId, userId);
        if (dto != null && dto.getAnswers() != null) {
            for (ExamSubmitDTO.AnswerItem item : dto.getAnswers()) {
                if (item.getQuestionId() != null) {
                    upsertAnswer(record.getId(), item.getQuestionId(), item.getUserAnswer());
                }
            }
        }
        return grade(record);
    }

    /**
     * 判分:客观题即时判定,主观题调用 AI 评分;最后汇总并生成 AI 学习建议。
     */
    private ExamResultVO grade(ExamRecord record) {
        Exam exam = examMapper.selectById(record.getExamId());
        List<ExamQuestionVO> paper = examQuestionMapper.selectPaperQuestions(record.getExamId());
        List<ExamAnswer> answers = examAnswerMapper.selectList(Wrappers.<ExamAnswer>lambdaQuery()
                .eq(ExamAnswer::getExamRecordId, record.getId()));
        Map<Long, ExamAnswer> answerMap = new LinkedHashMap<>();
        for (ExamAnswer answer : answers) {
            answerMap.put(answer.getQuestionId(), answer);
        }

        BigDecimal objective = BigDecimal.ZERO;
        BigDecimal subjective = BigDecimal.ZERO;
        int correctCount = 0;
        int answeredCount = 0;
        List<ExamResultVO.AnswerDetail> details = new ArrayList<>();

        for (ExamQuestionVO item : paper) {
            Question question = questionMapper.selectById(item.getQuestionId());
            if (question == null) {
                continue;
            }
            ExamAnswer answer = answerMap.get(item.getQuestionId());
            String userAnswer = answer == null ? null : answer.getUserAnswer();
            BigDecimal fullScore = item.getScore() == null ? new BigDecimal("5") : item.getScore();

            boolean correct = false;
            BigDecimal got = BigDecimal.ZERO;
            String feedback = null;

            if (userAnswer != null && !userAnswer.isBlank()) {
                answeredCount++;
                if (Constants.TYPE_SUBJECTIVE.equals(question.getType())) {
                    AiGradeResultVO grade = aiGradingService.grade(question.getId(), userAnswer, record.getUserId());
                    got = BigDecimal.valueOf(grade.getScore() == null ? 0 : grade.getScore());
                    feedback = grade.getFeedback();
                    correct = got.doubleValue() >= fullScore.doubleValue() * 0.6;
                    subjective = subjective.add(got);
                } else {
                    correct = answerChecker.isCorrect(question.getType(), question.getAnswer(), userAnswer);
                    got = correct ? fullScore : BigDecimal.ZERO;
                    objective = objective.add(got);
                }
                if (correct) {
                    correctCount++;
                }
                questionService.recordAnswer(question.getId(), correct);
                wrongBookService.record(record.getUserId(), question.getId(), userAnswer, correct);
            }

            if (answer != null) {
                answer.setScore(got);
                answer.setIsCorrect(correct ? 1 : 0);
                answer.setAiFeedback(feedback);
                answer.setGraded(1);
                examAnswerMapper.updateById(answer);
            }

            ExamResultVO.AnswerDetail detail = new ExamResultVO.AnswerDetail();
            detail.setQuestionId(question.getId());
            detail.setStem(question.getStem());
            detail.setType(question.getType());
            detail.setUserAnswer(userAnswer);
            detail.setCorrectAnswer(question.getAnswer());
            detail.setAnalysis(question.getAnalysis());
            detail.setIsCorrect(correct ? 1 : 0);
            detail.setScore(got);
            detail.setFullScore(fullScore);
            detail.setAiFeedback(feedback);
            details.add(detail);
        }

        BigDecimal total = objective.add(subjective);
        record.setObjectiveScore(objective);
        record.setSubjectiveScore(subjective);
        record.setTotalScore(total);
        record.setCorrectCount(correctCount);
        record.setStatus(Constants.RECORD_GRADED);
        record.setSubmitTime(LocalDateTime.now());

        // 基于本次及历史数据生成 AI 学习建议,失败不影响交卷
        try {
            var advice = aiAdviceService.generate(record.getUserId(), exam == null ? null : exam.getSubjectId());
            if (advice != null && advice.getContent() != null) {
                record.setAiComment(advice.getContent());
            }
        } catch (Exception ex) {
            log.warn("考试后生成学习建议失败:{}", ex.getMessage());
        }
        examRecordMapper.updateById(record);

        ExamResultVO vo = new ExamResultVO();
        vo.setRecordId(record.getId());
        vo.setExamId(record.getExamId());
        vo.setExamName(exam == null ? null : exam.getName());
        vo.setSubjectId(exam == null ? null : exam.getSubjectId());
        vo.setStartTime(record.getStartTime());
        vo.setSubmitTime(record.getSubmitTime());
        vo.setObjectiveScore(objective);
        vo.setSubjectiveScore(subjective);
        vo.setTotalScore(total);
        vo.setPassScore(exam == null ? null : exam.getPassScore());
        vo.setCorrectCount(correctCount);
        vo.setQuestionCount(paper.size());
        vo.setPass(exam != null && exam.getPassScore() != null && total.compareTo(exam.getPassScore()) >= 0);
        vo.setRate(exam == null || exam.getTotalScore() == null || exam.getTotalScore().doubleValue() == 0 ? 0.0
                : Math.round(total.doubleValue() * 10000 / exam.getTotalScore().doubleValue()) / 100.0);
        vo.setAiComment(record.getAiComment());
        vo.setStatus(record.getStatus());
        vo.setDetails(details);
        log.info("考试判分完成:examId={}, userId={}, 客观分={}, 主观分={}, 总分={}, 答对={}/{}",
                record.getExamId(), record.getUserId(), objective, subjective, total, correctCount, answeredCount);
        return vo;
    }

    @Override
    public ExamResultVO myResult(Long examId, Long userId) {
        ExamRecord record = examRecordMapper.selectOne(Wrappers.<ExamRecord>lambdaQuery()
                .eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getUserId, userId));
        if (record == null) {
            throw new BusinessException("你还没有参加这场考试");
        }
        if (Constants.RECORD_ONGOING.equals(record.getStatus())) {
            throw new BusinessException("考试尚未交卷");
        }
        Exam exam = examMapper.selectById(examId);
        List<ExamQuestionVO> paper = examQuestionMapper.selectPaperQuestions(examId);
        List<ExamAnswer> answers = examAnswerMapper.selectList(Wrappers.<ExamAnswer>lambdaQuery()
                .eq(ExamAnswer::getExamRecordId, record.getId()));
        Map<Long, ExamAnswer> answerMap = new LinkedHashMap<>();
        for (ExamAnswer answer : answers) {
            answerMap.put(answer.getQuestionId(), answer);
        }

        ExamResultVO vo = new ExamResultVO();
        vo.setRecordId(record.getId());
        vo.setExamId(examId);
        vo.setExamName(exam == null ? null : exam.getName());
        vo.setSubjectId(exam == null ? null : exam.getSubjectId());
        vo.setStartTime(record.getStartTime());
        vo.setSubmitTime(record.getSubmitTime());
        vo.setObjectiveScore(record.getObjectiveScore());
        vo.setSubjectiveScore(record.getSubjectiveScore());
        vo.setTotalScore(record.getTotalScore());
        vo.setPassScore(exam == null ? null : exam.getPassScore());
        vo.setCorrectCount(record.getCorrectCount());
        vo.setQuestionCount(paper.size());
        vo.setPass(exam != null && exam.getPassScore() != null
                && record.getTotalScore() != null && record.getTotalScore().compareTo(exam.getPassScore()) >= 0);
        vo.setRate(exam == null || exam.getTotalScore() == null || exam.getTotalScore().doubleValue() == 0 ? 0.0
                : Math.round(record.getTotalScore().doubleValue() * 10000 / exam.getTotalScore().doubleValue()) / 100.0);
        vo.setAiComment(record.getAiComment());
        vo.setStatus(record.getStatus());

        for (ExamQuestionVO item : paper) {
            Question question = questionMapper.selectById(item.getQuestionId());
            ExamAnswer answer = answerMap.get(item.getQuestionId());
            ExamResultVO.AnswerDetail detail = new ExamResultVO.AnswerDetail();
            detail.setQuestionId(item.getQuestionId());
            detail.setType(question == null ? null : question.getType());
            detail.setStem(question == null ? null : question.getStem());
            detail.setCorrectAnswer(question == null ? null : question.getAnswer());
            detail.setAnalysis(question == null ? null : question.getAnalysis());
            detail.setFullScore(item.getScore());
            if (answer != null) {
                detail.setUserAnswer(answer.getUserAnswer());
                detail.setIsCorrect(answer.getIsCorrect());
                detail.setScore(answer.getScore());
                detail.setAiFeedback(answer.getAiFeedback());
            }
            vo.getDetails().add(detail);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoSubmitExpired() {
        List<ExamRecord> expired = examRecordMapper.selectExpired();
        for (ExamRecord record : expired) {
            try {
                log.info("考试超时自动交卷:recordId={}, examId={}, userId={}",
                        record.getId(), record.getExamId(), record.getUserId());
                grade(record);
            } catch (Exception ex) {
                log.error("自动交卷失败:recordId={}", record.getId(), ex);
            }
        }
    }
}
