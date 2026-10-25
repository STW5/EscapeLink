package com.stw.escapelink.admin.dto;

import com.stw.escapelink.admin.domain.AdminAction;

import java.time.Instant;

public record AdminActionLogResponse(
        Long id,
        String adminUsername,
        AdminAction action,
        Long teamId,
        String teamName,
        Long quizId,
        String quizTitle,
        String detail,
        Instant createdAt
) {
}
