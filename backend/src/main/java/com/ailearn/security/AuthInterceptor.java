package com.ailearn.security;

import com.ailearn.common.Constants;
import com.ailearn.common.UnauthorizedException;
import com.ailearn.common.UserContext;
import com.ailearn.config.JwtProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 鉴权拦截器:解析令牌写入 {@link UserContext},并按注解做角色校验。
 *
 * <p>公开接口用 {@link PublicApi} 标注;需要管理员权限用 {@link AdminOnly} 标注。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        // 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        boolean isPublic = method.hasMethodAnnotation(PublicApi.class)
                || method.getBeanType().isAnnotationPresent(PublicApi.class);

        UserContext.LoginUser user = tokenProvider.parse(request.getHeader(jwtProperties.getHeader()));
        if (user != null) {
            UserContext.set(user);
        }

        if (isPublic) {
            return true;
        }
        if (user == null) {
            throw new UnauthorizedException("未登录或登录已过期,请重新登录");
        }

        boolean adminOnly = method.hasMethodAnnotation(AdminOnly.class)
                || method.getBeanType().isAnnotationPresent(AdminOnly.class);
        if (adminOnly && !Constants.ROLE_ADMIN.equals(user.getRole())) {
            throw new UnauthorizedException("需要管理员权限");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
