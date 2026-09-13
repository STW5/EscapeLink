package com.stw.escapelink.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stw.escapelink.global.exception.ErrorCode;
import com.stw.escapelink.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    // A private mapper, not the app-wide bean: this only ever serializes the
    // small ApiResponse error envelope, and Boot's default JSON mapper bean
    // type varies across versions (Jackson 2 vs. Jackson 3).
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        ErrorCode errorCode = ErrorCode.SESSION_EXPIRED;
        response.setStatus(errorCode.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.fail(errorCode.name(), errorCode.getDefaultMessage(), null)));
    }
}
