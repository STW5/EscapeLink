package com.stw.escapelink.admin.dto;

import java.time.Instant;

public record PendingSubmissionResponse(
        Long submissionId,
        Long teamId,
        String teamName,
        Long quizId,
        String quizTitle,
        int submissionVersion,
        Instant createdAt
) {
}
