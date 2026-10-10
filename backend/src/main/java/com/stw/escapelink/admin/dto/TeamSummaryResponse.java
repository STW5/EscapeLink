package com.stw.escapelink.admin.dto;

import com.stw.escapelink.quiz.dto.QuizListResponse;

public record TeamSummaryResponse(
        Long teamId,
        String teamName,
        String inviteToken,
        int currentRunNo,
        boolean finalStageUnlocked,
        boolean finalStageForced,
        QuizListResponse quizzes
) {
}
