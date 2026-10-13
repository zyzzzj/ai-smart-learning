package com.ailearn.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 对象存储配置(MinIO / 本地磁盘)。
 */
@Component
@ConfigurationProperties(prefix = "storage")
@Data
public class StorageProperties {

    /** minio | local */
    private String type = "minio";

    private Minio minio = new Minio();
    private Local local = new Local();

    @Data
    public static class Minio {
        private String endpoint = "http://localhost:9000";
        private String accessKey = "minioadmin";
        private String secretKey = "minioadmin";
        private String bucket = "ai-learning";
        /** 预签名 URL 有效期(秒) */
        private int presignedExpirySeconds = 3600;
    }

    @Data
    public static class Local {
        private String dir = "./upload";
        /** 本地存储的访问前缀:默认用相对路径,自动适应当前域名与端口 */
        private String urlPrefix = "/api/files/local";
    }

    public boolean useMinio() {
        return "minio".equalsIgnoreCase(type);
    }
}
