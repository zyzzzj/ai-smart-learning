package com.ailearn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 任务记录。
 */
@Data
@Schema(description = "AI 任务记录")
public class AiTaskVO {

    private Long id;
    private String type;
    private String provider;
    private String model;
    private String status;
    private Long costMs;
    private String error;
    private LocalDateTime createTime;
    private LocalDateTime finishTime;
}
