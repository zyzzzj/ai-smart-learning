package com.ailearn.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "交卷参数")
public class ExamSubmitDTO {

    @Schema(description = "最终作答(可选;考试过程中已通过 save 接口暂存的答案会自动参与判分)")
    private List<AnswerItem> answers = new ArrayList<>();

    @Data
    @Schema(description = "单题作答")
    public static class AnswerItem {
        private Long questionId;
        private String userAnswer;
    }
}
