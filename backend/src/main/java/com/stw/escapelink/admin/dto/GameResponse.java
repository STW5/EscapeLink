package com.stw.escapelink.admin.dto;

import com.stw.escapelink.game.domain.GameStatus;

import java.time.Instant;

public record GameResponse(
        Long id,
        String title,
        GameStatus status,
        Instant startAt,
        Instant endAt
) {
}
