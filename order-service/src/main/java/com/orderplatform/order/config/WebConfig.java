package com.orderplatform.order.config;

import com.orderplatform.order.interceptor.PermissionInterceptor;
import com.orderplatform.order.interceptor.UserInfoInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private UserInfoInterceptor userInfoInterceptor;

    @Autowired
    private PermissionInterceptor permissionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userInfoInterceptor)
                .addPathPatterns("/**")
                .order(0);

        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/order/admin/**")
                .order(1);
    }
}
