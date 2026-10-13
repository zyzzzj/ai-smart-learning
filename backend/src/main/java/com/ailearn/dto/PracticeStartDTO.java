package com.ailearn.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "开始刷题参数")
public class PracticeStartDTO {

    private Long subjectId;

    @Schema(description = "知识点 id(mode=KNOWLEDGE 时必填)")
    private Long knowledgePointId;

    @Schema(description = "刷题模式 RANDOM/KNOWLEDGE/WRONG/DIFFICULTY", example = "RANDOM")
    private String mode = "RANDOM";

    @Schema(description = "难度 1-5(mode=DIFFICULTY 时生效)")
    private Integer difficulty;

    @Schema(description = "题型,不传则混合")
    private String type;

    @Schema(description = "题目数量", example = "10")
    private Integer count = 10;

    @Schema(description = "分值(用于展示,实际以题目分值为准)")
    private BigDecimal score;
}
