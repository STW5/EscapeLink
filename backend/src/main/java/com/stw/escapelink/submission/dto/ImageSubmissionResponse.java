package com.stw.escapelink.submission.dto;

import com.stw.escapelink.quiz.domain.QuizProgressStatus;

public record ImageSubmissionResponse(
        Long submissionId,
        int submissionVersion,
        QuizProgressStatus status
) {
}
