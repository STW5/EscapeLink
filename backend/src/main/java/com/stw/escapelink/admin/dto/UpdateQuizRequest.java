package com.stw.escapelink.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UpdateQuizRequest(
        @NotBlank String title,
        @NotBlank String content,
        @Min(1) int orderNo,
        String hint,
        @Min(0) int hintDelaySeconds,
        /** TEXT quizzes only; null or blank keeps the current answer. */
        String answer
) {
}
