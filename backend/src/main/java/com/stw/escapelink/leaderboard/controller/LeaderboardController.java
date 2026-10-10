package com.stw.escapelink.leaderboard.controller;

import com.stw.escapelink.global.response.ApiResponse;
import com.stw.escapelink.leaderboard.dto.LeaderboardResponse;
import com.stw.escapelink.leaderboard.service.LeaderboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    /** Public, read-only — permitted without a TeamSession cookie (see SecurityConfig). */
    @GetMapping("/api/leaderboard")
    public ApiResponse<LeaderboardResponse> getLeaderboard(
            @RequestParam(required = false) Long gameId) {
        return ApiResponse.ok(leaderboardService.getLeaderboard(gameId));
    }
}
