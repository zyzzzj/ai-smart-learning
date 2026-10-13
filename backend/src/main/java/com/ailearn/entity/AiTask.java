package com.ailearn.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 任务记录(出题 / 主观题评分 / 学习建议)。
 */
@Data
@TableName("ai_task")
public class AiTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** GENERATE_QUESTION / GRADE_SUBJECTIVE / LEARNING_ADVICE */
    private String type;

    /** mock / ollama */
    private String provider;
    private String model;

    private Long subjectId;
    private Long knowledgePointId;
    private Long userId;

    /** 请求参数 JSON */
    private String requestParams;
    private String prompt;
    private String response;
    private String result;

    /** PENDING / RUNNING / SUCCESS / FAILED */
    private String status;
    private String error;
    private Long costMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private LocalDateTime finishTime;
}
