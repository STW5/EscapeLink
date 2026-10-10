package com.stw.escapelink.team.controller;

import com.stw.escapelink.global.response.ApiResponse;
import com.stw.escapelink.global.security.CurrentTeam;
import com.stw.escapelink.team.dto.ClearFinalStageResponse;
import com.stw.escapelink.team.service.FinalStageService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FinalStageController {

    private final FinalStageService finalStageService;

    public FinalStageController(FinalStageService finalStageService) {
        this.finalStageService = finalStageService;
    }

    @PostMapping("/api/final-stage/clear")
    public ApiResponse<ClearFinalStageResponse> clear(@AuthenticationPrincipal CurrentTeam currentTeam) {
        var clearedAt = finalStageService.clearFinalStage(currentTeam.teamId());
        return ApiResponse.ok(new ClearFinalStageResponse(clearedAt));
    }
}
