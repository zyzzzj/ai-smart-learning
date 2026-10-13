package com.ailearn.service.impl;

import com.ailearn.common.BusinessException;
import com.ailearn.common.Constants;
import com.ailearn.common.UserContext;
import com.ailearn.dto.ChangePasswordDTO;
import com.ailearn.dto.LoginDTO;
import com.ailearn.dto.RegisterDTO;
import com.ailearn.entity.SysUser;
import com.ailearn.mapper.SysUserMapper;
import com.ailearn.security.JwtTokenProvider;
import com.ailearn.service.AuthService;
import com.ailearn.vo.LoginVO;
import com.ailearn.vo.UserInfoVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 认证与账号实现。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(LoginDTO dto) {
        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用,请联系管理员");
        }
        if (!matches(dto.getPassword(), user)) {
            throw new BusinessException("用户名或密码错误");
        }

        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setLastLogin(LocalDateTime.now());
        userMapper.updateById(update);

        String token = tokenProvider.createToken(user.getId(), user.getUsername(), user.getNickname(), user.getRole());
        return LoginVO.builder()
                .token(token)
                .expireMinutes(tokenProvider.getExpireMinutes())
                .user(toVO(user))
                .build();
    }

    /**
     * 兼容历史明文密码:命中后用 BCrypt 重新加密落库,实现平滑升级。
     */
    private boolean matches(String rawPassword, SysUser user) {
        String stored = user.getPassword();
        if (stored == null) {
            return false;
        }
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, stored);
        }
        if (stored.equals(rawPassword)) {
            SysUser update = new SysUser();
            update.setId(user.getId());
            update.setPassword(passwordEncoder.encode(rawPassword));
            userMapper.updateById(update);
            user.setPassword(update.getPassword());
            log.info("用户 {} 的密码已自动升级为 BCrypt", user.getUsername());
            return true;
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO register(RegisterDTO dto) {
        Long exists = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() == null || dto.getNickname().isBlank()
                ? dto.getUsername() : dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setRole(Constants.ROLE_STUDENT);
        user.setStatus(1);
        userMapper.insert(user);
        return toVO(user);
    }

    @Override
    public UserInfoVO currentUser() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        return toVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordDTO dto) {
        SysUser user = userMapper.selectById(UserContext.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!matches(dto.getOldPassword(), user)) {
            throw new BusinessException("原密码不正确");
        }
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(update);
    }

    private UserInfoVO toVO(SysUser user) {
        return UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .lastLogin(user.getLastLogin())
                .createTime(user.getCreateTime())
                .build();
    }
}
