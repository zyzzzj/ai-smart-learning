package com.ailearn.service.impl;

import com.ailearn.common.BusinessException;
import com.ailearn.common.CacheNames;
import com.ailearn.common.Constants;
import com.ailearn.common.PageResult;
import com.ailearn.common.QuestionValidator;
import com.ailearn.dto.QuestionDTO;
import com.ailearn.dto.QuestionOption;
import com.ailearn.dto.QuestionQueryDTO;
import com.ailearn.entity.KnowledgePoint;
import com.ailearn.entity.Question;
import com.ailearn.entity.Subject;
import com.ailearn.mapper.KnowledgePointMapper;
import com.ailearn.mapper.QuestionMapper;
import com.ailearn.mapper.SubjectMapper;
import com.ailearn.service.HotQuestionService;
import com.ailearn.service.QuestionService;
import com.ailearn.vo.QuestionDetailVO;
import com.ailearn.vo.QuestionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 题库实现。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionServiceImpl implements QuestionService {

    private final QuestionMapper questionMapper;
    private final SubjectMapper subjectMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final QuestionDetailCacheService detailCacheService;
    private final HotQuestionService hotQuestionService;
    private final QuestionValidator questionValidator;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<QuestionVO> page(QuestionQueryDTO query) {
        LambdaQueryWrapper<Question> wrapper = Wrappers.<Question>lambdaQuery()
                .eq(query.getSubjectId() != null, Question::getSubjectId, query.getSubjectId())
                .eq(query.getKnowledgePointId() != null, Question::getKnowledgePointId, query.getKnowledgePointId())
                .eq(query.getType() != null && !query.getType().isBlank(), Question::getType, query.getType())
                .eq(query.getDifficulty() != null, Question::getDifficulty, query.getDifficulty())
                .eq(query.getSource() != null && !query.getSource().isBlank(), Question::getSource, query.getSource())
                .eq(query.getStatus() != null, Question::getStatus, query.getStatus())
                .like(query.getKeyword() != null && !query.getKeyword().isBlank(), Question::getStem, query.getKeyword())
                .orderByDesc(Question::getId);

        Page<Question> page = questionMapper.selectPage(Page.of(query.getPage(), query.getSize()), wrapper);

        // 批量补齐科目 / 知识点名称,避免 N+1
        Map<Long, String> subjectNames = new HashMap<>();
        Map<Long, String> kpNames = new HashMap<>();
        for (Question q : page.getRecords()) {
            if (q.getSubjectId() != null && !subjectNames.containsKey(q.getSubjectId())) {
                Subject s = subjectMapper.selectById(q.getSubjectId());
                subjectNames.put(q.getSubjectId(), s == null ? null : s.getName());
            }
            if (q.getKnowledgePointId() != null && !kpNames.containsKey(q.getKnowledgePointId())) {
                KnowledgePoint kp = knowledgePointMapper.selectById(q.getKnowledgePointId());
                kpNames.put(q.getKnowledgePointId(), kp == null ? null : kp.getName());
            }
        }

        List<QuestionVO> records = new ArrayList<>();
        for (Question q : page.getRecords()) {
            QuestionVO vo = new QuestionVO();
            vo.setId(q.getId());
            vo.setSubjectId(q.getSubjectId());
            vo.setSubjectName(subjectNames.get(q.getSubjectId()));
            vo.setKnowledgePointId(q.getKnowledgePointId());
            vo.setKnowledgePointName(kpNames.get(q.getKnowledgePointId()));
            vo.setType(q.getType());
            vo.setStem(q.getStem());
            vo.setOptions(parseOptions(q.getOptions()));
            vo.setDifficulty(q.getDifficulty());
            vo.setScore(q.getScore());
            vo.setTags(q.getTags());
            vo.setSource(q.getSource());
            vo.setStatus(q.getStatus());
            vo.setViewCount(q.getViewCount());
            vo.setCorrectCount(q.getCorrectCount());
            vo.setCreateTime(q.getCreateTime());
            records.add(vo);
        }
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    public QuestionDetailVO detail(Long id) {
        QuestionDetailVO vo = detailCacheService.load(id);
        // 浏览量只在 Redis 排行里累计,避免每次查看都写库
        hotQuestionService.record(id, 1);
        return vo;
    }

    @Override
    public List<Question> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return questionMapper.selectBatchIds(new LinkedHashSet<>(ids));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {CacheNames.QUESTION_DETAIL, CacheNames.HOT_QUESTIONS}, allEntries = true)
    public Question create(QuestionDTO dto) {
        validate(dto);
        Question question = new Question();
        applyDto(question, dto);
        question.setSource(Constants.SOURCE_MANUAL);
        question.setViewCount(0);
        question.setCorrectCount(0);
        questionMapper.insert(question);
        return question;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {CacheNames.QUESTION_DETAIL, CacheNames.HOT_QUESTIONS}, allEntries = true)
    public Question update(QuestionDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("题目 id 不能为空");
        }
        Question exists = questionMapper.selectById(dto.getId());
        if (exists == null) {
            throw new BusinessException("题目不存在");
        }
        validate(dto);
        Question question = new Question();
        question.setId(dto.getId());
        applyDto(question, dto);
        questionMapper.updateById(question);
        return questionMapper.selectById(dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {CacheNames.QUESTION_DETAIL, CacheNames.HOT_QUESTIONS}, allEntries = true)
    public void remove(Long id) {
        questionMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {CacheNames.QUESTION_DETAIL, CacheNames.HOT_QUESTIONS}, allEntries = true)
    public void removeBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        questionMapper.deleteByIds(new LinkedHashSet<>(ids));
    }

    @Override
    public void recordAnswer(Long questionId, boolean correct) {
        if (questionId == null) {
            return;
        }
        questionMapper.increaseCounters(questionId, correct ? 1 : 0);
        // 答对权重更高,热门排行里体现区分度
        hotQuestionService.record(questionId, correct ? 1.0 : 0.5);
    }

    /**
     * 题型与答案的基础校验(与 AI 出题共用 QuestionValidator,保证规则一致)。
     */
    private void validate(QuestionDTO dto) {
        String reason = questionValidator.validate(dto.getType(), dto.getOptions(), dto.getAnswer());
        if (reason != null) {
            throw new BusinessException(reason);
        }
        if (dto.getScore() == null || dto.getScore().compareTo(BigDecimal.ZERO) <= 0) {
            dto.setScore(new BigDecimal("5.0"));
        }
    }

    private void applyDto(Question question, QuestionDTO dto) {
        question.setSubjectId(dto.getSubjectId());
        question.setKnowledgePointId(dto.getKnowledgePointId());
        question.setType(dto.getType());
        question.setStem(dto.getStem());
        question.setAnswer(dto.getAnswer());
        question.setAnalysis(dto.getAnalysis());
        question.setDifficulty(dto.getDifficulty() == null ? 3 : dto.getDifficulty());
        question.setScore(dto.getScore());
        question.setTags(dto.getTags());
        question.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        question.setOptions(writeOptions(dto.getOptions()));
    }

    private String writeOptions(List<QuestionOption> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(options);
        } catch (Exception ex) {
            throw new BusinessException("选项序列化失败");
        }
    }

    private List<QuestionOption> parseOptions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<QuestionOption>>() {
            });
        } catch (Exception ex) {
            log.warn("选项解析失败:{}", json);
            return List.of();
        }
    }
}
