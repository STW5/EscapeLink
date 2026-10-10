package com.stw.escapelink.admin.controller;

import com.stw.escapelink.admin.dto.TeamSummaryResponse;
import com.stw.escapelink.admin.service.AdminTeamService;
import com.stw.escapelink.global.response.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AdminTeamController {

    private final AdminTeamService adminTeamService;

    public AdminTeamController(AdminTeamService adminTeamService) {
        this.adminTeamService = adminTeamService;
    }

    @GetMapping("/api/admin/teams")
    public ApiResponse<List<TeamSummaryResponse>> listTeams() {
        return ApiResponse.ok(adminTeamService.listTeams());
    }

    @PostMapping("/api/admin/teams/{teamId}/quizzes/{quizId}/force-complete")
    public ApiResponse<Void> forceComplete(Authentication authentication,
                                            @PathVariable Long teamId, @PathVariable Long quizId) {
        adminTeamService.forceCompleteQuiz(authentication.getName(), teamId, quizId);
        return ApiResponse.ok();
    }

    @PostMapping("/api/admin/teams/{teamId}/reset")
    public ApiResponse<Void> resetTeam(Authentication authentication, @PathVariable Long teamId) {
        adminTeamService.resetTeam(authentication.getName(), teamId);
        return ApiResponse.ok();
    }

    @PostMapping("/api/admin/teams/{teamId}/force-final-stage")
    public ApiResponse<Void> forceFinalStage(Authentication authentication, @PathVariable Long teamId) {
        adminTeamService.forceFinalStage(authentication.getName(), teamId);
        return ApiResponse.ok();
    }
}
