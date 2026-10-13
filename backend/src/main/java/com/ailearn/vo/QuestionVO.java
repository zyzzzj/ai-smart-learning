package com.ailearn.vo;

import com.ailearn.dto.QuestionOption;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 题目列表视图(不含答案,可用于练习/考试作答)。
 */
@Data
@Schema(description = "题目(不含答案)")
public class QuestionVO {

    private Long id;
    private Long subjectId;
    private String subjectName;
    private Long knowledgePointId;
    private String knowledgePointName;

    private String type;
    private String stem;
    private List<QuestionOption> options;
    private Integer difficulty;
    private BigDecimal score;
    private String tags;
    private String source;
    private Integer status;

    @Schema(description = "作答次数")
    private Integer viewCount;

    @Schema(description = "答对次数")
    private Integer correctCount;

    @Schema(description = "正确率(%)")
    private Double correctRate;

    private LocalDateTime createTime;

    public Double getCorrectRate() {
        if (viewCount == null || viewCount == 0) {
            return 0.0;
        }
        return Math.round((correctCount == null ? 0 : correctCount) * 10000.0 / viewCount) / 100.0;
    }
}
