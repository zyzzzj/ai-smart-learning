package com.ailearn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 考试试卷(学生端作答页)。
 */
@Data
@Schema(description = "考试试卷")
public class ExamPaperVO {

    private Long examId;
    private String name;
    private String description;
    private Long subjectId;
    private Integer duration;
    private BigDecimal totalScore;
    private BigDecimal passScore;

    private Long recordId;
    private LocalDateTime startTime;

    @Schema(description = "剩余作答时间(秒)")
    private Long remainingSeconds;

    @Schema(description = "已暂存答案的题数")
    private Integer savedCount;

    private List<ExamQuestionVO> questions = new ArrayList<>();
}
