package com.ailearn.dto;

import com.ailearn.common.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "题目查询参数")
public class QuestionQueryDTO extends PageQuery {

    private Long subjectId;
    private Long knowledgePointId;

    @Schema(description = "题型")
    private String type;

    private Integer difficulty;

    @Schema(description = "来源 MANUAL/AI/IMPORT")
    private String source;

    @Schema(description = "题干关键词")
    private String keyword;

    private Integer status;
}
