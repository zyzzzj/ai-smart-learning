package com.ailearn.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Schema(description = "AI 自动组卷参数")
public class ExamAutoComposeDTO {

    @NotBlank(message = "考试名称不能为空")
    private String name;

    @NotNull(message = "科目不能为空")
    private Long subjectId;

    private String description;

    @Schema(description = "时长(分钟)", example = "60")
    private Integer duration = 60;

    @Schema(description = "及格分")
    private BigDecimal passScore;

    @Schema(description = "总题数", example = "10")
    private Integer questionCount = 10;

    @Schema(description = "知识点 id 列表,不传则整个科目随机")
    private java.util.List<Long> knowledgePointIds;

    @Schema(description = "难度配比,如 {\"2\":3,\"3\":4,\"4\":3} 表示难度2 取3题、难度3 取4题、难度4 取3题")
    private Map<Integer, Integer> difficultyDistribution = new LinkedHashMap<>();

    @Schema(description = "题型配比,如 {\"SINGLE\":6,\"MULTIPLE\":2,\"JUDGE\":1,\"SUBJECTIVE\":1}")
    private Map<String, Integer> typeDistribution = new LinkedHashMap<>();
}
