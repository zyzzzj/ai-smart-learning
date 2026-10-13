package com.ailearn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 考试列表项(学生端 / 管理端)。
 */
@Data
@Schema(description = "考试列表项")
public class ExamVO {

    private Long id;
    private String name;
    private Long subjectId;
    private String subjectName;
    private String description;
    private Integer duration;
    private BigDecimal totalScore;
    private BigDecimal passScore;
    private Integer questionCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private LocalDateTime createTime;

    @Schema(description = "我的考试记录状态:未参加/ONGOING/GRADED 等")
    private String myRecordStatus;

    @Schema(description = "我的得分")
    private BigDecimal myScore;
}
