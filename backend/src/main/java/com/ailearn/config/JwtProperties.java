package com.ailearn.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置。
 */
@Component
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {

    /** 签名密钥(生产环境务必替换) */
    private String secret = "ailearn-secret-key-please-change-in-production-2026";
    /** 过期时间(分钟) */
    private long expireMinutes = 1440;
    /** 令牌请求头 */
    private String header = "Authorization";
    /** 令牌前缀 */
    private String prefix = "Bearer ";
}
