package com.stw.escapelink.admin.controller;

import com.stw.escapelink.global.response.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AdminAuthController {

    @GetMapping("/api/admin/me")
    public ApiResponse<Map<String, String>> me(Authentication authentication) {
        return ApiResponse.ok(Map.of("username", authentication.getName()));
    }
}
