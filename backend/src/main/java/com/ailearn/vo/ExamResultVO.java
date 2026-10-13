package com.ailearn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 考试结果(交卷后 / 查看已结束考试)。
 */
@Data
@Schema(description = "考试结果")
public class ExamResultVO {

    private Long recordId;
    private Long examId;
    private String examName;
    private Long subjectId;

    private LocalDateTime startTime;
    private LocalDateTime submitTime;

    private BigDecimal objectiveScore;
    private BigDecimal subjectiveScore;
    private BigDecimal totalScore;
    private BigDecimal passScore;
    private Integer correctCount;
    private Integer questionCount;

    @Schema(description = "是否及格")
    private Boolean pass;

    @Schema(description = "得分率(%)")
    private Double rate;

    @Schema(description = "AI 学习建议(基于本次考试表现)")
    private String aiComment;

    private String status;

    private List<AnswerDetail> details = new ArrayList<>();

    @Data
    @Schema(description = "逐题结果")
    public static class AnswerDetail {
        private Long questionId;
        private String stem;
        private String type;
        private String userAnswer;
        private String correctAnswer;
        private String analysis;
        private Integer isCorrect;
        private BigDecimal score;
        private BigDecimal fullScore;
        private String aiFeedback;
    }
}
