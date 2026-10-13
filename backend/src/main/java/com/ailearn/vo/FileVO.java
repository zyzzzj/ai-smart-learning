package com.ailearn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资源文件信息。
 */
@Data
@Schema(description = "资源文件")
public class FileVO {

    private Long id;
    private String bucket;
    private String objectKey;
    private String originalName;
    private String contentType;
    private Long size;

    @Schema(description = "VIDEO/IMAGE/DOC/OTHER")
    private String category;

    @Schema(description = "关联业务:QUESTION/SUBJECT/KNOWLEDGE/AVATAR")
    private String bizType;
    private Long bizId;

    @Schema(description = "访问地址(MinIO 为预签名 URL,本地为签名直链)")
    private String url;

    private Long uploaderId;
    private LocalDateTime createTime;

    @Schema(description = "大小(可读)")
    private String sizeText;
}
