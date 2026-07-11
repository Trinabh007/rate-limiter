package com.RateMesh.ratemesh.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.RateMesh.ratemesh.interceptor.AdminAuthInterceptor;
import com.RateMesh.ratemesh.interceptor.RateLimiterInterceptor;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer{
    private final  AdminAuthInterceptor adminAuthInterceptor;
    private final RateLimiterInterceptor rateLimiterInterceptor;
    public WebMvcConfig(AdminAuthInterceptor adminAuthInterceptor, RateLimiterInterceptor rateLimiterInterceptor) {
        this.adminAuthInterceptor = adminAuthInterceptor;
        this.rateLimiterInterceptor = rateLimiterInterceptor;
    }
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimiterInterceptor).addPathPatterns("/**").excludePathPatterns("/admin/**","/actuator/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/api-docs/**",
                "/v3/api-docs/**");
        registry.addInterceptor(adminAuthInterceptor)
            .addPathPatterns("/admin/**");
    }
    
}
