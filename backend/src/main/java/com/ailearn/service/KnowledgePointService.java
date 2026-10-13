package com.ailearn.service;

import com.ailearn.dto.KnowledgePointDTO;
import com.ailearn.entity.KnowledgePoint;
import com.ailearn.vo.KnowledgePointTreeVO;

import java.util.List;

/**
 * 知识点。
 */
public interface KnowledgePointService {

    /** 知识点树(带缓存) */
    List<KnowledgePointTreeVO> tree(Long subjectId);

    /** 平铺列表 */
    List<KnowledgePoint> list(Long subjectId);

    KnowledgePoint getById(Long id);

    KnowledgePoint save(KnowledgePointDTO dto);

    KnowledgePoint update(KnowledgePointDTO dto);

    void remove(Long id);
}
