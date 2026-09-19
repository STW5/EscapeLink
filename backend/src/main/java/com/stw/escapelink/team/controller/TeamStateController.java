package com.stw.escapelink.team.controller;

import com.stw.escapelink.global.response.ApiResponse;
import com.stw.escapelink.global.security.CurrentTeam;
import com.stw.escapelink.team.dto.TeamStateResponse;
import com.stw.escapelink.team.service.TeamSessionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TeamStateController {

    private final TeamSessionService teamSessionService;

    public TeamStateController(TeamSessionService teamSessionService) {
        this.teamSessionService = teamSessionService;
    }

    @GetMapping("/api/team-state")
    public ApiResponse<TeamStateResponse> getState(@AuthenticationPrincipal CurrentTeam currentTeam) {
        return ApiResponse.ok(teamSessionService.getState(currentTeam.teamId()));
    }
}
