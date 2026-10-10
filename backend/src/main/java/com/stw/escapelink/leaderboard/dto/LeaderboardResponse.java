package com.stw.escapelink.leaderboard.dto;

import java.util.List;

public record LeaderboardResponse(
        Long gameId,
        String gameTitle,
        List<LeaderboardEntry> teams
) {
}
