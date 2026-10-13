package com.ailearn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "当前用户信息")
public class UserInfoVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;

    @Schema(description = "角色 STUDENT/ADMIN")
    private String role;

    private LocalDateTime lastLogin;
    private LocalDateTime createTime;
}
