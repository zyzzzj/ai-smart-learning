package com.ailearn.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "题目新增/修改参数")
public class QuestionDTO {

    private Long id;

    @NotNull(message = "所属科目不能为空")
    private Long subjectId;

    @Schema(description = "知识点 id")
    private Long knowledgePointId;

    @NotBlank(message = "题型不能为空")
    @Schema(description = "SINGLE/MULTIPLE/JUDGE/FILL/SUBJECTIVE", example = "SINGLE")
    private String type;

    @NotBlank(message = "题干不能为空")
    private String stem;

    @Schema(description = "选项(选择题必填)")
    private List<QuestionOption> options;

    @NotBlank(message = "参考答案不能为空")
    private String answer;

    private String analysis;

    @Min(value = 1, message = "难度范围 1-5")
    @Max(value = 5, message = "难度范围 1-5")
    private Integer difficulty = 3;

    @Schema(description = "建议分值")
    private BigDecimal score;

    @Schema(description = "标签,逗号分隔")
    private String tags;

    private Integer status = 1;
}
