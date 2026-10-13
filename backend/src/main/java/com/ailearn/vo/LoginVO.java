package com.ailearn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "登录返回")
public class LoginVO {

    @Schema(description = "JWT 令牌")
    private String token;

    @Schema(description = "过期时间(分钟)")
    private Long expireMinutes;

    private UserInfoVO user;
}
