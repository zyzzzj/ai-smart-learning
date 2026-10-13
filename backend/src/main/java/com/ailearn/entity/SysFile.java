package com.ailearn.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资源文件(MinIO 对象或本地文件)。
 */
@Data
@TableName("sys_file")
public class SysFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String bucket;
    private String objectKey;
    private String originalName;
    private String contentType;
    private Long size;

    /** VIDEO / IMAGE / DOC / OTHER */
    private String category;

    /** QUESTION / SUBJECT / KNOWLEDGE / AVATAR */
    private String bizType;
    private Long bizId;
    private String url;
    private Long uploaderId;

    @TableLogic
    @TableField(select = false)
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
