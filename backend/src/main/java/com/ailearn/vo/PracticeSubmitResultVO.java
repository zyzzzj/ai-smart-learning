package com.ailearn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 单题作答结果。
 */
@Data
@Schema(description = "作答结果")
public class PracticeSubmitResultVO {

    private Long questionId;
    private String type;

    @Schema(description = "是否答对(主观题由 AI 评分,得分 >= 60% 视为通过)")
    private Boolean correct;

    private Double score;
    private Double fullScore;

    @Schema(description = "参考答案")
    private String correctAnswer;

    @Schema(description = "解析")
    private String analysis;

    @Schema(description = "主观题 AI 评语")
    private String aiFeedback;

    @Schema(description = "主观题命中的要点")
    private List<String> hitPoints;

    @Schema(description = "主观题遗漏的要点")
    private List<String> missingPoints;

    @Schema(description = "本次会话累计:已答 / 答对 / 得分")
    private Integer answeredCount;
    private Integer correctCount;
    private Double totalScore;

    @Schema(description = "是否已加入错题本")
    private Boolean addedToWrongBook;
}
