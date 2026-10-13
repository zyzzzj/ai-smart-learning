package com.ailearn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 学习统计概览(学习报告页 + AI 建议输入)。
 */
@Data
@Schema(description = "学习统计")
public class LearningStatsVO {

    @Schema(description = "累计作答题数")
    private Long totalAnswered;

    @Schema(description = "累计答对题数")
    private Long totalCorrect;

    @Schema(description = "总正确率(%)")
    private Double accuracy;

    @Schema(description = "刷题会话数")
    private Long sessionCount;

    @Schema(description = "错题本未掌握数")
    private Long wrongCount;

    @Schema(description = "错题本已掌握数")
    private Long masteredCount;

    @Schema(description = "参加考试次数")
    private Long examCount;

    @Schema(description = "考试平均分")
    private Double examAvgScore;

    @Schema(description = "答题总耗时(秒)")
    private Long totalTimeSpent;

    @Schema(description = "各知识点正确率")
    private List<KnowledgeStatVO> knowledgeStats = new ArrayList<>();

    @Schema(description = "最近 7 天趋势")
    private List<DailyStatVO> recentTrend = new ArrayList<>();

    @Schema(description = "薄弱知识点(正确率 < 70%)")
    private List<String> weakPoints = new ArrayList<>();
}
