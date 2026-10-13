package com.ailearn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成绩排行。
 */
@Data
@Schema(description = "成绩排行")
public class ScoreRankVO {

    private Long id;
    private Long userId;
    private String nickname;
    private BigDecimal totalScore;
    private LocalDateTime submitTime;
    private String status;
}
