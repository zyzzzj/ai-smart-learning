package com.ailearn.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 学习建议。
 */
@Data
@TableName("learning_advice")
public class LearningAdvice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long subjectId;
    private String content;

    /** AI / RULE */
    private String source;
    private Long aiTaskId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
