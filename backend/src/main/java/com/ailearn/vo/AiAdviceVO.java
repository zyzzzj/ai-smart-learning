package com.ailearn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 学习建议。
 */
@Data
@Schema(description = "AI 学习建议")
public class AiAdviceVO {

    private Long id;
    private Long taskId;
    private String provider;
    private String model;

    @Schema(description = "薄弱知识点")
    private List<String> weakPoints;

    @Schema(description = "建议全文(Markdown)")
    private String content;

    private LocalDateTime createTime;
}
