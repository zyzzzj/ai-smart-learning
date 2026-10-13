package com.ailearn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 错题本条目。
 */
@Data
@Schema(description = "错题本条目")
public class WrongQuestionVO {

    private Long id;
    private Long questionId;
    private Integer wrongCount;
    private Integer mastered;
    private LocalDateTime lastWrongTime;
    private String lastAnswer;

    private String stem;
    private String type;
    private Integer difficulty;
    private String analysis;
    private String referenceAnswer;
    private String knowledgePointName;
}
