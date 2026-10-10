package com.stw.escapelink.admin.controller;

import com.stw.escapelink.admin.dto.CreateGameRequest;
import com.stw.escapelink.admin.dto.GameResponse;
import com.stw.escapelink.admin.service.AdminGameService;
import com.stw.escapelink.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AdminGameController {

    private final AdminGameService adminGameService;

    public AdminGameController(AdminGameService adminGameService) {
        this.adminGameService = adminGameService;
    }

    @PostMapping("/api/admin/games")
    public ApiResponse<GameResponse> createGame(@Valid @RequestBody CreateGameRequest request) {
        return ApiResponse.ok(adminGameService.createGame(request));
    }

    @GetMapping("/api/admin/games")
    public ApiResponse<List<GameResponse>> listGames() {
        return ApiResponse.ok(adminGameService.listGames());
    }

    @PostMapping("/api/admin/games/{gameId}/start")
    public ApiResponse<GameResponse> startGame(@PathVariable Long gameId) {
        return ApiResponse.ok(adminGameService.startGame(gameId));
    }

    @PostMapping("/api/admin/games/{gameId}/finish")
    public ApiResponse<GameResponse> finishGame(@PathVariable Long gameId) {
        return ApiResponse.ok(adminGameService.finishGame(gameId));
    }
}
