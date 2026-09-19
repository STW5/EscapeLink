package com.stw.escapelink.quiz.dto;

import java.util.List;

public record QuizListResponse(
        Long gameId,
        Long teamId,
        List<QuizListItemResponse> quizzes
) {
}
