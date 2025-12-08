package com.example.common.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private JWTInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor).addPathPatterns("/**")
                .excludePathPatterns("/")
                .excludePathPatterns(
                        "/login",
                        "/register",
                        "/api/auth/**",  // 排除认证相关接口（登录、注册）
                        "/files/**",
                        "/notice/selectAll",
                        "/advertise/selectAll",
                        "/industry/selectAll",
                        "/position/selectAll",
                        "/position/selectById/**",
                        "/position/recommend",
                        "/employ/selectById/**",
                        "/doc.html",
                        "/doc.html/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/swagger-resources/**",
                        "/webjars/**"
                );
    }

}
