package com.stw.escapelink.quiz.dto;

import com.stw.escapelink.quiz.domain.QuizProgressStatus;
import com.stw.escapelink.quiz.domain.QuizType;

public record QuizListItemResponse(
        Long id,
        String title,
        QuizType type,
        QuizProgressStatus status
) {
}
