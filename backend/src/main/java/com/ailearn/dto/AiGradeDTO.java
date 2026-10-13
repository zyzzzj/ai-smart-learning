package com.ailearn.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "AI 主观题评分参数")
public class AiGradeDTO {

    @NotNull(message = "题目 id 不能为空")
    private Long questionId;

    @NotBlank(message = "学生答案不能为空")
    @Schema(description = "学生作答内容")
    private String studentAnswer;
}
