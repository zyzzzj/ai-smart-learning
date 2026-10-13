package com.ailearn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 预签名直传信息(前端拿到后直接 PUT 到 uploadUrl,不经过后端)。
 */
@Data
@Schema(description = "预签名上传信息")
public class UploadInfoVO {

    private Long fileId;
    private String storageType;
    private String bucket;
    private String objectKey;

    @Schema(description = "上传地址(PUT 该地址即可完成上传)")
    private String uploadUrl;

    @Schema(description = "上传完成后的访问地址")
    private String downloadUrl;

    @Schema(description = "有效期(秒)")
    private Integer expiresIn;
}
