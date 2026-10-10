package com.stw.escapelink.admin.dto;

import com.stw.escapelink.quiz.domain.QuizType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateQuizRequest(
        @NotBlank String title,
        @NotBlank String content,
        @NotNull QuizType type,
        @Min(1) int orderNo,
        String hint,
        int hintDelaySeconds,
        /** Required when type is TEXT; ignored for IMAGE. */
        String answer
) {
}
