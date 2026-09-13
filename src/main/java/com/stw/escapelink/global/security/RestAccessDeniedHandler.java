package com.stw.escapelink.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stw.escapelink.global.exception.ErrorCode;
import com.stw.escapelink.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    // A private mapper, not the app-wide bean: this only ever serializes the
    // small ApiResponse error envelope, and Boot's default JSON mapper bean
    // type varies across versions (Jackson 2 vs. Jackson 3).
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
        response.setStatus(errorCode.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.fail(errorCode.name(), errorCode.getDefaultMessage(), null)));
    }
}
