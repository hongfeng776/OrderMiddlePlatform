package com.orderplatform.order.interceptor;

import com.orderplatform.common.context.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class UserInfoInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userIdHeader = request.getHeader("X-User-Id");
        String userRole = request.getHeader("X-User-Role");
        String userPermissions = request.getHeader("X-User-Permissions");

        if (userIdHeader != null && !userIdHeader.isEmpty()) {
            try {
                Long userId = Long.parseLong(userIdHeader);
                UserContext.setUserId(userId);
                UserContext.setRole(userRole != null ? userRole : "USER");
                UserContext.setPermissions(userPermissions);
            } catch (NumberFormatException e) {
                log.warn("无效的用户ID格式: {}", userIdHeader);
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.clear();
    }
}
