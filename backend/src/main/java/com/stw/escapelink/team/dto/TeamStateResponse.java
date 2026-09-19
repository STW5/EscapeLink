package com.stw.escapelink.team.dto;

import com.stw.escapelink.game.domain.GameStatus;

import java.time.Instant;

public record TeamStateResponse(
        Long teamId,
        String teamName,
        int currentRunNo,
        Long gameId,
        GameStatus gameStatus,
        Instant gameStartAt,
        Instant gameEndAt
) {
}
