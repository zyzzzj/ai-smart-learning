package com.ailearn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识点树节点。
 */
@Data
@Schema(description = "知识点树节点")
public class KnowledgePointTreeVO {

    private Long id;
    private Long subjectId;
    private Long parentId;
    private String name;
    private String description;
    private Integer difficulty;
    private Integer sort;

    @Schema(description = "该知识点下的题目数量")
    private Integer questionCount;

    private List<KnowledgePointTreeVO> children = new ArrayList<>();
}
