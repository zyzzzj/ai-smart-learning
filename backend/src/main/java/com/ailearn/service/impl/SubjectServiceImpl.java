package com.ailearn.service.impl;

import com.ailearn.common.BusinessException;
import com.ailearn.common.CacheNames;
import com.ailearn.entity.Subject;
import com.ailearn.mapper.SubjectMapper;
import com.ailearn.service.SubjectService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 科目实现:列表走 Redis 缓存,写操作清理缓存。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubjectServiceImpl implements SubjectService {

    private final SubjectMapper subjectMapper;

    @Override
    @Cacheable(cacheNames = CacheNames.SUBJECT_LIST, key = "'enabled'")
    public List<Subject> listEnabled() {
        log.debug("查询启用科目(未命中缓存)");
        return subjectMapper.selectList(Wrappers.<Subject>lambdaQuery()
                .eq(Subject::getStatus, 1)
                .orderByAsc(Subject::getSort)
                .orderByAsc(Subject::getId));
    }

    @Override
    public List<Subject> listAll() {
        return subjectMapper.selectList(Wrappers.<Subject>lambdaQuery()
                .orderByAsc(Subject::getSort)
                .orderByAsc(Subject::getId));
    }

    @Override
    public Subject getById(Long id) {
        Subject subject = subjectMapper.selectById(id);
        if (subject == null) {
            throw new BusinessException("科目不存在");
        }
        return subject;
    }

    @Override
    @CacheEvict(cacheNames = {CacheNames.SUBJECT_LIST, CacheNames.KNOWLEDGE_TREE}, allEntries = true)
    public Subject save(Subject subject) {
        if (!StringUtils.hasText(subject.getName())) {
            throw new BusinessException("科目名称不能为空");
        }
        Long exists = subjectMapper.selectCount(Wrappers.<Subject>lambdaQuery()
                .eq(Subject::getName, subject.getName().trim()));
        if (exists != null && exists > 0) {
            throw new BusinessException("科目名称已存在:" + subject.getName());
        }
        if (subject.getStatus() == null) {
            subject.setStatus(1);
        }
        if (subject.getSort() == null) {
            subject.setSort(0);
        }
        subject.setName(subject.getName().trim());
        subjectMapper.insert(subject);
        return subject;
    }

    @Override
    @CacheEvict(cacheNames = {CacheNames.SUBJECT_LIST, CacheNames.KNOWLEDGE_TREE}, allEntries = true)
    public Subject update(Subject subject) {
        if (subject.getId() == null) {
            throw new BusinessException("科目 id 不能为空");
        }
        if (!StringUtils.hasText(subject.getName())) {
            throw new BusinessException("科目名称不能为空");
        }
        subject.setName(subject.getName().trim());
        subjectMapper.updateById(subject);
        return subjectMapper.selectById(subject.getId());
    }

    @Override
    @CacheEvict(cacheNames = {CacheNames.SUBJECT_LIST, CacheNames.KNOWLEDGE_TREE}, allEntries = true)
    public void remove(Long id) {
        Long children = subjectMapper.selectCount(Wrappers.<Subject>lambdaQuery().eq(Subject::getId, id));
        if (children == null || children == 0) {
            throw new BusinessException("科目不存在");
        }
        subjectMapper.deleteById(id);
    }
}
