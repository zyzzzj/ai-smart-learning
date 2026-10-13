package com.ailearn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 热门题目(缓存与首页推荐使用)。
 */
@Data
@Schema(description = "热门题目")
public class HotQuestionVO {

    private Long id;
    private String stem;
    private String type;
    private Integer difficulty;
    private Integer viewCount;
    private Integer correctCount;
    private String subjectName;
    private String knowledgePointName;
}
