package com.ailearn.service.impl;

import com.ailearn.common.CacheNames;
import com.ailearn.entity.KnowledgePoint;
import com.ailearn.entity.Question;
import com.ailearn.entity.Subject;
import com.ailearn.mapper.KnowledgePointMapper;
import com.ailearn.mapper.QuestionMapper;
import com.ailearn.mapper.SubjectMapper;
import com.ailearn.service.HotQuestionService;
import com.ailearn.vo.HotQuestionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 热门题目实现:Redis ZSet 记录热度,查询时按分数倒序取 TopN。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HotQuestionServiceImpl implements HotQuestionService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final QuestionMapper questionMapper;
    private final SubjectMapper subjectMapper;
    private final KnowledgePointMapper knowledgePointMapper;

    @Override
    public void record(Long questionId, double score) {
        if (questionId == null) {
            return;
        }
        try {
            redisTemplate.opsForZSet().incrementScore(CacheNames.HOT_QUESTION_ZSET, String.valueOf(questionId), score);
        } catch (Exception ex) {
            // 排行是加分项,Redis 异常不应影响主流程
            log.warn("热门题目排行写入失败:{}", ex.getMessage());
        }
    }

    @Override
    @Cacheable(cacheNames = CacheNames.HOT_QUESTIONS, key = "#subjectId == null ? 'all:' + #limit : #subjectId + ':' + #limit")
    public List<HotQuestionVO> topN(Long subjectId, int limit) {
        log.debug("查询热门题目(未命中缓存):subjectId={}, limit={}", subjectId, limit);
        int size = Math.max(1, Math.min(limit, 50));

        // 1. 优先从 Redis ZSet 取排行
        List<Long> ids = new ArrayList<>();
        try {
            Set<ZSetOperations.TypedTuple<Object>> tuples =
                    redisTemplate.opsForZSet().reverseRangeWithScores(CacheNames.HOT_QUESTION_ZSET, 0, size * 3L);
            if (tuples != null) {
                for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
                    if (tuple.getValue() == null) {
                        continue;
                    }
                    ids.add(Long.valueOf(String.valueOf(tuple.getValue())));
                }
            }
        } catch (Exception ex) {
            log.warn("读取热门排行失败,回退数据库:{}", ex.getMessage());
        }

        List<HotQuestionVO> result = new ArrayList<>();
        if (!ids.isEmpty()) {
            List<Question> questions = questionMapper.selectBatchIds(new LinkedHashSet<>(ids));
            Map<Long, Question> questionMap = new HashMap<>();
            for (Question q : questions) {
                questionMap.put(q.getId(), q);
            }
            for (Long id : ids) {
                Question q = questionMap.get(id);
                if (q == null || q.getStatus() == null || q.getStatus() == 0) {
                    continue;
                }
                if (subjectId != null && !subjectId.equals(q.getSubjectId())) {
                    continue;
                }
                result.add(toVO(q));
                if (result.size() >= size) {
                    break;
                }
            }
        }

        // 2. ZSet 没有数据时回退到数据库统计
        if (result.isEmpty()) {
            List<HotQuestionVO> fromDb = questionMapper.selectHotQuestions(subjectId, size);
            return fromDb == null ? List.of() : fromDb;
        }
        return result;
    }

    private HotQuestionVO toVO(Question q) {
        HotQuestionVO vo = new HotQuestionVO();
        vo.setId(q.getId());
        vo.setStem(q.getStem());
        vo.setType(q.getType());
        vo.setDifficulty(q.getDifficulty());
        vo.setViewCount(q.getViewCount());
        vo.setCorrectCount(q.getCorrectCount());
        Subject subject = q.getSubjectId() == null ? null : subjectMapper.selectById(q.getSubjectId());
        vo.setSubjectName(subject == null ? null : subject.getName());
        KnowledgePoint kp = q.getKnowledgePointId() == null ? null
                : knowledgePointMapper.selectById(q.getKnowledgePointId());
        vo.setKnowledgePointName(kp == null ? null : kp.getName());
        return vo;
    }
}
