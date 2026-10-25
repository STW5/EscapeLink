package com.stw.escapelink.admin.controller;

import com.stw.escapelink.admin.dto.AdminActionLogResponse;
import com.stw.escapelink.admin.service.AdminActionLogService;
import com.stw.escapelink.global.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AdminActionLogController {

    private final AdminActionLogService adminActionLogService;

    public AdminActionLogController(AdminActionLogService adminActionLogService) {
        this.adminActionLogService = adminActionLogService;
    }

    @GetMapping("/api/admin/action-logs")
    public ApiResponse<List<AdminActionLogResponse>> list(@RequestParam(required = false) Long teamId,
                                                           @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(adminActionLogService.list(teamId, limit));
    }
}
