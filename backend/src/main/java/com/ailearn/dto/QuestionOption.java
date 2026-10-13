package com.ailearn.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 选择题选项。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "选项")
public class QuestionOption {

    @Schema(description = "选项标识 A/B/C/D", example = "A")
    private String key;

    @Schema(description = "选项内容")
    private String content;
}
