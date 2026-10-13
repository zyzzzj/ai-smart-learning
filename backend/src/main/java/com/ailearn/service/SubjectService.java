package com.ailearn.service;

import com.ailearn.entity.Subject;

import java.util.List;

/**
 * 科目 / 课程。
 */
public interface SubjectService {

    /** 启用中的科目列表(带缓存) */
    List<Subject> listEnabled();

    /** 全部科目(管理端) */
    List<Subject> listAll();

    Subject getById(Long id);

    Subject save(Subject subject);

    Subject update(Subject subject);

    void remove(Long id);
}
