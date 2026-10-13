package com.ailearn.vo;

import com.ailearn.dto.QuestionOption;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 试卷题目(不含答案,作答时下发)。
 */
@Data
@Schema(description = "试卷题目")
public class ExamQuestionVO {

    private Long questionId;
    private Long knowledgePointId;
    private String knowledgePointName;
    private String type;
    private String stem;
    private List<QuestionOption> options;
    private Integer difficulty;

    @Schema(description = "本场考试中该题分值")
    private BigDecimal score;

    private Integer sort;

    @Schema(description = "学生已暂存的作答")
    private String userAnswer;
}
