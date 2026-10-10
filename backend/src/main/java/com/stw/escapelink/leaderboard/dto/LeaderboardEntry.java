package com.stw.escapelink.leaderboard.dto;

import java.time.Instant;

/**
 * Public-safe only: never includes the answer, inviteToken, admin info,
 * raw submitted images, or session data.
 */
public record LeaderboardEntry(
        String teamName,
        int completedQuizCount,
        int totalQuizCount,
        boolean finalStageReached,
        Instant clearTime
) {
}
