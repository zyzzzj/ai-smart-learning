package com.ailearn.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "提交单题作答参数")
public class PracticeSubmitDTO {

    @NotNull(message = "题目 id 不能为空")
    private Long questionId;

    @NotBlank(message = "答案不能为空")
    @Schema(description = "学生作答,选择题填 A 或 A,C;判断题填 对/错;主观题填文本")
    private String userAnswer;

    @Schema(description = "本题耗时(秒)")
    private Integer timeSpent = 0;
}
