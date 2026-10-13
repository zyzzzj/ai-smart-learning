package com.ailearn.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 题目。
 */
@Data
@TableName("question")
public class Question {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long subjectId;
    private Long knowledgePointId;

    /** SINGLE / MULTIPLE / JUDGE / FILL / SUBJECTIVE */
    private String type;

    private String stem;
    /** JSON 字符串:[{"key":"A","content":"..."}] */
    private String options;
    private String answer;
    private String analysis;
    private Integer difficulty;
    private BigDecimal score;

    /** MANUAL / AI / IMPORT */
    private String source;

    private String tags;
    private Long aiTaskId;

    private Integer viewCount;
    private Integer correctCount;
    private Integer status;
    private Long createUser;

    @TableLogic
    @TableField(select = false)
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
