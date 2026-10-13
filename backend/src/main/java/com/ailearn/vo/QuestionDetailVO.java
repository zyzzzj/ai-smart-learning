package com.ailearn.vo;

import com.ailearn.dto.QuestionOption;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 题目详情(含答案与解析,管理端与作答后查看)。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "题目详情")
public class QuestionDetailVO extends QuestionVO {

    @Schema(description = "参考答案")
    private String answer;

    @Schema(description = "解析")
    private String analysis;

    public List<QuestionOption> getOptions() {
        return super.getOptions();
    }
}
