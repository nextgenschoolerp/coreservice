package com.kvs.core.config.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.kvs.core.service.JwtTokenService;

@AllArgsConstructor
@Component
public class JwtTokenInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtTokenService jwtTokenService;

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) throws Exception {
        String header = request.getHeader("Authorization");
        String token = null;

        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        }

        if (token != null && validateToken(token)) {
            // Token is valid, continue the request
            return true;
        } else {
            // Token is invalid, return 401 Unauthorized
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }

    public boolean validateToken(String token) {
        try {
            // Validate the token using your logic, e.g., using JwtUtil
            jwtTokenService.validateToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
