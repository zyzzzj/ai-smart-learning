package com.ailearn.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "考试新增/修改参数")
public class ExamDTO {

    private Long id;

    @NotBlank(message = "考试名称不能为空")
    private String name;

    @NotNull(message = "科目不能为空")
    private Long subjectId;

    private String description;

    @Schema(description = "时长(分钟)", example = "60")
    private Integer duration = 60;

    @Schema(description = "及格分", example = "60")
    private BigDecimal passScore;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Schema(description = "试卷题目(手动组卷时传)")
    private List<ExamQuestionItem> questions = new ArrayList<>();

    @Data
    @Schema(description = "试卷题目项")
    public static class ExamQuestionItem {
        @NotNull
        private Long questionId;
        @Schema(description = "该题分值,不传取题目建议分值")
        private BigDecimal score;
        private Integer sort;
    }
}
