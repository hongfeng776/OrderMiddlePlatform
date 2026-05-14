package com.orderplatform.order.interceptor;

import com.orderplatform.common.annotation.RequiresPermission;
import com.orderplatform.common.context.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequiresPermission methodAnnotation = handlerMethod.getMethodAnnotation(RequiresPermission.class);
        RequiresPermission classAnnotation = handlerMethod.getBeanType().getAnnotation(RequiresPermission.class);

        String requiredPermission = null;
        if (methodAnnotation != null) {
            requiredPermission = methodAnnotation.value();
        } else if (classAnnotation != null) {
            requiredPermission = classAnnotation.value();
        }

        if (requiredPermission == null || requiredPermission.isEmpty()) {
            return true;
        }

        String userPermissions = UserContext.getPermissions();
        String userRole = UserContext.getRole();

        if ("ADMIN".equals(userRole)) {
            return true;
        }

        if (userPermissions != null && !userPermissions.isEmpty()) {
            Set<String> permissionSet = new HashSet<>(Arrays.asList(userPermissions.split(",")));
            if (permissionSet.contains(requiredPermission)) {
                return true;
            }
        }

        log.warn("权限不足，用户: {}, 需要权限: {}", UserContext.getUserId(), requiredPermission);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\": 403, \"message\": \"权限不足\"}");
        return false;
    }
}
