package com.ailearn.service.impl;

import com.ailearn.common.BusinessException;
import com.ailearn.common.CacheNames;
import com.ailearn.dto.KnowledgePointDTO;
import com.ailearn.entity.KnowledgePoint;
import com.ailearn.mapper.KnowledgePointMapper;
import com.ailearn.mapper.QuestionMapper;
import com.ailearn.service.KnowledgePointService;
import com.ailearn.vo.KnowledgePointTreeVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识点实现:树结构查询走 Redis 缓存。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgePointServiceImpl implements KnowledgePointService {

    private final KnowledgePointMapper knowledgePointMapper;
    private final QuestionMapper questionMapper;

    @Override
    @Cacheable(cacheNames = CacheNames.KNOWLEDGE_TREE, key = "#subjectId == null ? 'all' : #subjectId")
    public List<KnowledgePointTreeVO> tree(Long subjectId) {
        log.debug("构建知识点树(未命中缓存):subjectId={}", subjectId);
        List<KnowledgePoint> all = list(subjectId);

        // 各知识点题量
        Map<Long, Integer> countMap = new HashMap<>();
        List<Map<String, Object>> counts = questionMapper.countGroupByKnowledge(subjectId);
        for (Map<String, Object> row : counts) {
            Object kpId = row.get("knowledgePointId");
            Object total = row.get("total");
            if (kpId != null && total != null) {
                countMap.put(((Number) kpId).longValue(), ((Number) total).intValue());
            }
        }

        // 组装树
        Map<Long, KnowledgePointTreeVO> nodeMap = new LinkedHashMap<>();
        for (KnowledgePoint kp : all) {
            KnowledgePointTreeVO node = new KnowledgePointTreeVO();
            BeanUtils.copyProperties(kp, node);
            node.setQuestionCount(countMap.getOrDefault(kp.getId(), 0));
            nodeMap.put(kp.getId(), node);
        }
        List<KnowledgePointTreeVO> roots = new ArrayList<>();
        for (KnowledgePoint kp : all) {
            KnowledgePointTreeVO node = nodeMap.get(kp.getId());
            KnowledgePointTreeVO parent = nodeMap.get(kp.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        // 父节点题量累加子节点,便于前端展示
        for (KnowledgePointTreeVO root : roots) {
            accumulate(root);
        }
        return roots;
    }

    private int accumulate(KnowledgePointTreeVO node) {
        int total = node.getQuestionCount() == null ? 0 : node.getQuestionCount();
        for (KnowledgePointTreeVO child : node.getChildren()) {
            total += accumulate(child);
        }
        node.setQuestionCount(total);
        return total;
    }

    @Override
    public List<KnowledgePoint> list(Long subjectId) {
        return knowledgePointMapper.selectList(Wrappers.<KnowledgePoint>lambdaQuery()
                .eq(subjectId != null, KnowledgePoint::getSubjectId, subjectId)
                .eq(KnowledgePoint::getStatus, 1)
                .orderByAsc(KnowledgePoint::getSort)
                .orderByAsc(KnowledgePoint::getId));
    }

    @Override
    public KnowledgePoint getById(Long id) {
        KnowledgePoint kp = knowledgePointMapper.selectById(id);
        if (kp == null) {
            throw new BusinessException("知识点不存在");
        }
        return kp;
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.KNOWLEDGE_TREE, allEntries = true)
    public KnowledgePoint save(KnowledgePointDTO dto) {
        KnowledgePoint kp = new KnowledgePoint();
        BeanUtils.copyProperties(dto, kp);
        if (kp.getParentId() == null) {
            kp.setParentId(0L);
        }
        knowledgePointMapper.insert(kp);
        return kp;
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.KNOWLEDGE_TREE, allEntries = true)
    public KnowledgePoint update(KnowledgePointDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("知识点 id 不能为空");
        }
        if (dto.getId().equals(dto.getParentId())) {
            throw new BusinessException("父知识点不能是自己");
        }
        KnowledgePoint kp = new KnowledgePoint();
        BeanUtils.copyProperties(dto, kp);
        knowledgePointMapper.updateById(kp);
        return knowledgePointMapper.selectById(dto.getId());
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.KNOWLEDGE_TREE, allEntries = true)
    public void remove(Long id) {
        Long childCount = knowledgePointMapper.selectCount(Wrappers.<KnowledgePoint>lambdaQuery()
                .eq(KnowledgePoint::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BusinessException("该知识点下还有子知识点,请先删除子节点");
        }
        Long questionCount = knowledgePointMapper.selectCount(Wrappers.<KnowledgePoint>lambdaQuery()
                .eq(KnowledgePoint::getId, id));
        if (questionCount == null || questionCount == 0) {
            throw new BusinessException("知识点不存在");
        }
        knowledgePointMapper.deleteById(id);
    }
}
