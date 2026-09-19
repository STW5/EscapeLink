package com.stw.escapelink.quiz.dto;

import com.stw.escapelink.quiz.domain.QuizProgressStatus;

import java.time.Instant;

public record AnswerResponse(
        boolean correct,
        QuizProgressStatus status,
        Instant solvedAt
) {
}
