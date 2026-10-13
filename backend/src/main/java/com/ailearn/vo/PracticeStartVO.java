package com.ailearn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 开始刷题返回:会话 id + 本次题目(不含答案)。
 */
@Data
@Schema(description = "刷题会话")
public class PracticeStartVO {

    private Long sessionId;
    private String mode;
    private Integer total;

    @Schema(description = "本次抽到的题目(不含答案)")
    private List<QuestionVO> questions = new ArrayList<>();
}
