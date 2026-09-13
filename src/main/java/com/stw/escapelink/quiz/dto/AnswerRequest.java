package com.stw.escapelink.quiz.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AnswerRequest(
        @NotBlank String requestId,
        @NotBlank String answer,
        @Min(1) int runNo
) {
}
