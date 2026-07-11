package com.RateMesh.ratemesh.interceptor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
@Component
public class AdminAuthInterceptor  implements HandlerInterceptor{
    @Value("${admin.api.key}")
    private String adminApiKey;
     @Override
    public boolean preHandle(HttpServletRequest request, 
                             HttpServletResponse response, 
                             Object handler) throws Exception {
        String key = request.getHeader("X-Admin-Key");
        if (key == null || !key.equals(adminApiKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid or missing admin API key");
            return false;
        }
        return true;
    }
}
