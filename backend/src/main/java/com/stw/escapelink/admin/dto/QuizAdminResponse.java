package com.stw.escapelink.admin.dto;

import com.stw.escapelink.quiz.domain.QuizType;

/** Admin-only view — unlike participant-facing DTOs, this carries the answer. */
public record QuizAdminResponse(
        Long id,
        Long gameId,
        String title,
        String content,
        QuizType type,
        int orderNo,
        String hint,
        int hintDelaySeconds,
        String answer
) {
}
