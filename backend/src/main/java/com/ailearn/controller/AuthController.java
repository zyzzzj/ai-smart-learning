package com.ailearn.controller;

import com.ailearn.common.Result;
import com.ailearn.dto.ChangePasswordDTO;
import com.ailearn.dto.LoginDTO;
import com.ailearn.dto.RegisterDTO;
import com.ailearn.security.PublicApi;
import com.ailearn.service.AuthService;
import com.ailearn.vo.LoginVO;
import com.ailearn.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证", description = "登录、注册、当前用户")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @PublicApi
    @Operation(summary = "登录")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @PostMapping("/register")
    @PublicApi
    @Operation(summary = "注册学生账号")
    public Result<UserInfoVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(authService.register(dto));
    }

    @GetMapping("/me")
    @Operation(summary = "当前登录用户")
    public Result<UserInfoVO> me() {
        return Result.success(authService.currentUser());
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePassword(dto);
        return Result.success();
    }
}
