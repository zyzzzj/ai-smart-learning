package com.ailearn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 知识点维度统计(学习画像 / AI 建议输入)。
 */
@Data
@Schema(description = "知识点统计")
public class KnowledgeStatVO {

    private Long knowledgePointId;
    private String knowledgePointName;
    private Integer total;
    private Integer correct;

    /** 正确率(0-100),由 total/correct 计算 */
    public Double getRate() {
        if (total == null || total == 0) {
            return 0.0;
        }
        return Math.round((correct == null ? 0 : correct) * 10000.0 / total) / 100.0;
    }
}
