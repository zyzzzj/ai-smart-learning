package com.ailearn.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "AI 出题参数")
public class AiGenerateQuestionDTO {

    @NotNull(message = "科目不能为空")
    private Long subjectId;

    @Schema(description = "知识点 id,不传则按科目整体出题")
    private Long knowledgePointId;

    @Schema(description = "题型 SINGLE/MULTIPLE/JUDGE/FILL/SUBJECTIVE", example = "SINGLE")
    private String type = "SINGLE";

    @Min(value = 1, message = "难度范围 1-5")
    @Max(value = 5, message = "难度范围 1-5")
    @Schema(description = "难度 1-5", example = "3")
    private Integer difficulty = 3;

    @Min(value = 1, message = "数量至少 1 道")
    @Max(value = 10, message = "单次最多生成 10 道")
    @Schema(description = "生成数量", example = "3")
    private Integer count = 3;

    @Schema(description = "每道题分值")
    private BigDecimal score;

    @Schema(description = "标签")
    private String tags;

    @Schema(description = "是否直接入库,false 时仅预览", example = "true")
    private Boolean autoSave = true;
}
