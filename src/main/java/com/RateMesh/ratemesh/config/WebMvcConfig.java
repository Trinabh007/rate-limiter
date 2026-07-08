package com.RateMesh.ratemesh.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.RateMesh.ratemesh.interceptor.RateLimiterInterceptor;
@Configuration
public class WebMvcConfig implements WebMvcConfigurer{

    private final RateLimiterInterceptor rateLimiterInterceptor;
    public WebMvcConfig(RateLimiterInterceptor rateLimiterInterceptor) {
        this.rateLimiterInterceptor = rateLimiterInterceptor;
    }
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimiterInterceptor).addPathPatterns("/**").excludePathPatterns("/admin/**");
    }
    
}
