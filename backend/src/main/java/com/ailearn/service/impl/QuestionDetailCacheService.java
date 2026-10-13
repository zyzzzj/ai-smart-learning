package com.ailearn.service.impl;

import com.ailearn.common.BusinessException;
import com.ailearn.common.CacheNames;
import com.ailearn.entity.KnowledgePoint;
import com.ailearn.entity.Question;
import com.ailearn.entity.Subject;
import com.ailearn.mapper.KnowledgePointMapper;
import com.ailearn.mapper.QuestionMapper;
import com.ailearn.mapper.SubjectMapper;
import com.ailearn.vo.QuestionDetailVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 题目详情缓存:单独成 Bean,避免同类内部调用导致 @Cacheable 失效。
 */
@Service
@Slf4j
public class QuestionDetailCacheService {

    private final QuestionMapper questionMapper;
    private final SubjectMapper subjectMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final ObjectMapper objectMapper;

    public QuestionDetailCacheService(QuestionMapper questionMapper,
                                      SubjectMapper subjectMapper,
                                      KnowledgePointMapper knowledgePointMapper,
                                      ObjectMapper objectMapper) {
        this.questionMapper = questionMapper;
        this.subjectMapper = subjectMapper;
        this.knowledgePointMapper = knowledgePointMapper;
        this.objectMapper = objectMapper;
    }

    @Cacheable(cacheNames = CacheNames.QUESTION_DETAIL, key = "#id")
    public QuestionDetailVO load(Long id) {
        log.debug("加载题目详情(未命中缓存):id={}", id);
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw new BusinessException("题目不存在");
        }
        QuestionDetailVO vo = new QuestionDetailVO();
        vo.setId(question.getId());
        vo.setSubjectId(question.getSubjectId());
        vo.setKnowledgePointId(question.getKnowledgePointId());
        vo.setType(question.getType());
        vo.setStem(question.getStem());
        vo.setAnswer(question.getAnswer());
        vo.setAnalysis(question.getAnalysis());
        vo.setDifficulty(question.getDifficulty());
        vo.setScore(question.getScore());
        vo.setTags(question.getTags());
        vo.setSource(question.getSource());
        vo.setStatus(question.getStatus());
        vo.setViewCount(question.getViewCount());
        vo.setCorrectCount(question.getCorrectCount());
        vo.setCreateTime(question.getCreateTime());
        vo.setOptions(parseOptions(question.getOptions()));

        Subject subject = question.getSubjectId() == null ? null : subjectMapper.selectById(question.getSubjectId());
        vo.setSubjectName(subject == null ? null : subject.getName());
        KnowledgePoint kp = question.getKnowledgePointId() == null ? null
                : knowledgePointMapper.selectById(question.getKnowledgePointId());
        vo.setKnowledgePointName(kp == null ? null : kp.getName());
        return vo;
    }

    private java.util.List<com.ailearn.dto.QuestionOption> parseOptions(String json) {
        if (json == null || json.isBlank()) {
            return java.util.List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<java.util.List<com.ailearn.dto.QuestionOption>>() {
            });
        } catch (Exception ex) {
            log.warn("题目选项 JSON 解析失败:{}", json);
            return java.util.List.of();
        }
    }
}
