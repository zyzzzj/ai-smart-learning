package com.ailearn.common;

/**
 * 当前登录用户上下文(由鉴权拦截器写入,请求结束清理)。
 */
public class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getUserId();
    }

    /** 未登录时返回 null,避免在公开接口里抛异常 */
    public static String getRole() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getRole();
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(getRole());
    }

    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 登录用户信息。
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class LoginUser {
        private Long userId;
        private String username;
        private String nickname;
        private String role;
    }
}
