package com.ailearn.vo;

import com.ailearn.dto.QuestionOption;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI 生成的一道题(含校验结果)。
 */
@Data
@Schema(description = "AI 生成的题目")
public class GeneratedQuestionVO {

    private String type;
    private String stem;
    private List<QuestionOption> options;
    private String answer;
    private String analysis;
    private Integer difficulty;
    private BigDecimal score;
    private String tags;

    @Schema(description = "是否通过结构校验")
    private Boolean valid;

    @Schema(description = "校验失败原因")
    private String invalidReason;

    @Schema(description = "入库后的题目 id(autoSave=true 时返回)")
    private Long questionId;
}
