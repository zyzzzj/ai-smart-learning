package com.ailearn.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 刷题答题明细。
 */
@Data
@TableName("practice_record")
public class PracticeRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;
    private Long userId;
    private Long questionId;
    private String userAnswer;
    /** 1 对 0 错 null 待判定(主观题) */
    private Integer isCorrect;
    private BigDecimal score;
    private String aiFeedback;
    private Integer timeSpent;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
