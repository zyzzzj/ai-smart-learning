package com.ailearn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 每日作答统计(学习趋势图)。
 */
@Data
@Schema(description = "每日作答统计")
public class DailyStatVO {

    @Schema(description = "日期 yyyy-MM-dd")
    private String date;

    private Integer answered;
    private Integer correct;

    public Double getAccuracy() {
        if (answered == null || answered == 0) {
            return 0.0;
        }
        return Math.round((correct == null ? 0 : correct) * 10000.0 / answered) / 100.0;
    }
}
