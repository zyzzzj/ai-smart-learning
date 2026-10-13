package com.ailearn.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 刷题会话。
 */
@Data
@TableName("practice_session")
public class PracticeSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long subjectId;
    private Long knowledgePointId;

    /** RANDOM / KNOWLEDGE / WRONG / DIFFICULTY */
    private String mode;

    /** 本次抽题 id 列表(JSON 数组字符串) */
    private String questionIds;

    private Integer questionCount;
    private Integer answeredCount;
    private Integer correctCount;
    private BigDecimal totalScore;

    /** ONGOING / FINISHED */
    private String status;

    private LocalDateTime startTime;
    private LocalDateTime finishTime;
}
