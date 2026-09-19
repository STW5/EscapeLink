package com.stw.escapelink.quiz.dto;

import com.stw.escapelink.quiz.domain.QuizProgressStatus;
import com.stw.escapelink.quiz.domain.QuizType;

import java.time.Instant;

public record QuizDetailResponse(
        Long id,
        String title,
        String content,
        QuizType type,
        QuizProgressStatus status,
        boolean hintAvailable,
        Instant hintAvailableAt,
        String hint
) {
}
