package com.ailearn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 刷题会话详情。
 */
@Data
@Schema(description = "刷题会话详情")
public class PracticeSessionVO {

    private Long id;
    private Long subjectId;
    private Long knowledgePointId;
    private String mode;
    private Integer questionCount;
    private Integer answeredCount;
    private Integer correctCount;
    private BigDecimal totalScore;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;

    @Schema(description = "正确率(%)")
    private Double accuracy;

    private List<RecordItem> records = new ArrayList<>();

    @Data
    @Schema(description = "作答明细")
    public static class RecordItem {
        private Long questionId;
        private String stem;
        private String type;
        private String userAnswer;
        private Integer isCorrect;
        private BigDecimal score;
        private String aiFeedback;
        private Integer timeSpent;
        private String correctAnswer;
        private String analysis;
    }

    public Double getAccuracy() {
        if (answeredCount == null || answeredCount == 0) {
            return 0.0;
        }
        return Math.round((correctCount == null ? 0 : correctCount) * 10000.0 / answeredCount) / 100.0;
    }
}
