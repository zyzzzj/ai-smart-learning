package com.ailearn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * AI 主观题评分结果。
 */
@Data
@Schema(description = "AI 评分结果")
public class AiGradeResultVO {

    private Long taskId;
    private String provider;
    private String model;
    private Long questionId;

    @Schema(description = "得分")
    private Double score;

    @Schema(description = "满分")
    private Double fullScore;

    @Schema(description = "命中的要点")
    private List<String> hitPoints;

    @Schema(description = "遗漏的要点")
    private List<String> missingPoints;

    @Schema(description = "评语与改进建议")
    private String feedback;

    @Schema(description = "模型对该评分的置信度")
    private Double confidence;

    private Long costMs;
}
