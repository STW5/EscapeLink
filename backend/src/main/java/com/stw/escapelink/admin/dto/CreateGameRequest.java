package com.stw.escapelink.admin.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record CreateGameRequest(
        @NotBlank String title,
        Instant startAt,
        Instant endAt
) {
}
