package com.ailearn.service;

import com.ailearn.dto.ChangePasswordDTO;
import com.ailearn.dto.LoginDTO;
import com.ailearn.dto.RegisterDTO;
import com.ailearn.vo.LoginVO;
import com.ailearn.vo.UserInfoVO;

/**
 * 认证与账号。
 */
public interface AuthService {

    /** 登录,返回令牌与用户信息 */
    LoginVO login(LoginDTO dto);

    /** 注册学生账号 */
    UserInfoVO register(RegisterDTO dto);

    /** 当前登录用户信息 */
    UserInfoVO currentUser();

    /** 修改密码 */
    void changePassword(ChangePasswordDTO dto);
}
