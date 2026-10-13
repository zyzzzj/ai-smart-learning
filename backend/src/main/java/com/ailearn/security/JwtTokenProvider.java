package com.ailearn.security;

import com.ailearn.common.UserContext;
import com.ailearn.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 生成与解析。
 */
@Component
@Slf4j
public class JwtTokenProvider {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        // HS256 要求密钥不短于 32 字节,不足时右侧补齐,避免启动即报错
        String secret = properties.getSecret();
        StringBuilder sb = new StringBuilder(secret == null ? "" : secret);
        while (sb.length() < 32) {
            sb.append('0');
        }
        this.key = Keys.hmacShaKeyFor(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成令牌。
     */
    public String createToken(Long userId, String username, String nickname, String role) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + properties.getExpireMinutes() * 60_000L);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("nickname", nickname)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expire)
                .signWith(key)
                .compact();
    }

    /**
     * 解析令牌为登录用户;令牌非法/过期时返回 null。
     */
    public UserContext.LoginUser parse(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String raw = token.trim();
        String prefix = properties.getPrefix();
        if (prefix != null && !prefix.isBlank() && raw.startsWith(prefix.trim())) {
            raw = raw.substring(prefix.trim().length()).trim();
        } else if (raw.startsWith("Bearer ")) {
            raw = raw.substring(7).trim();
        }
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(raw).getPayload();
            return new UserContext.LoginUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get("username", String.class),
                    claims.get("nickname", String.class),
                    claims.get("role", String.class));
        } catch (Exception ex) {
            log.debug("令牌解析失败:{}", ex.getMessage());
            return null;
        }
    }

    public long getExpireMinutes() {
        return properties.getExpireMinutes();
    }
}
