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
 * 考试作答明细。
 */
@Data
@TableName("exam_answer")
public class ExamAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long examRecordId;
    private Long questionId;
    private String userAnswer;
    private Integer isCorrect;
    private BigDecimal score;
    private String aiFeedback;
    private Integer graded;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
